package com.jeppeman.locallydynamic;

import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.google.android.play.core.splitinstall.SplitInstallRequest;
import com.jeppeman.locallydynamic.net.ListUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static com.jeppeman.locallydynamic.serialization.StringUtils.joinToString;

class ApkDownloadRequestFactory {
    static ApkDownloadRequest create(
            @NonNull Context context,
            @NonNull Executor executor,
            @NonNull Logger logger,
            @NonNull LocallyDynamicConfigurationDto configuration,
            @NonNull SplitInstallRequest splitInstallRequest,
            @NonNull String title,
            @NonNull String description,
            @NonNull ApkDownloadRequest.StatusListener statusListener
    ) {
        return new ApkDownloadRequestImpl(
                context,
                executor,
                logger,
                configuration,
                splitInstallRequest,
                title,
                description,
                statusListener,
                (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE)
        );
    }
}

/**
 * Wrapper for [DownloadManager] which downloads splits from the requested features via
 * the locally dynamic server
 */
interface ApkDownloadRequest {
    void start();

    void cancel();

    abstract class Status {
        final long totalBytes;
        final long bytesDownloaded;
        final int errorCode;

        Status(long totalBytes, long bytesDownloaded) {
            this(totalBytes, bytesDownloaded, -1);
        }

        Status(long totalBytes, long bytesDownloaded, int errorCode) {
            this.totalBytes = totalBytes;
            this.bytesDownloaded = bytesDownloaded;
            this.errorCode = errorCode;
        }

        static class Pending extends Status {
            Pending(long totalBytes, long bytesDownloaded) {
                super(totalBytes, bytesDownloaded);
            }
        }

        static class Unknown extends Status {
            final int state;

            Unknown(long totalBytes, long bytesDownloaded, int state) {
                super(totalBytes, bytesDownloaded);
                this.state = state;
            }
        }

        static class Failed extends Status {
            Failed(long totalBytes, long bytesDownloaded, int code) {
                super(totalBytes, bytesDownloaded, code);
            }
        }

        static class Canceled extends Status {
            Canceled(long totalBytes, long bytesDownloaded) {
                super(totalBytes, bytesDownloaded);
            }
        }

        static class Paused extends Status {
            Paused(long totalBytes, long bytesDownloaded) {
                super(totalBytes, bytesDownloaded);
            }
        }

        static class Running extends Status {
            Running(long totalBytes, long bytesDownloaded) {
                super(totalBytes, bytesDownloaded);
            }
        }

        static class Successful extends Status {
            final List<File> apks;

            Successful(long totalBytes, long bytesDownloaded, List<File> apks) {
                super(totalBytes, bytesDownloaded);
                this.apks = apks;
            }
        }

        @NonNull
        @Override
        public String toString() {
            return String.format(
                    Locale.ENGLISH,
                    "%s(totalBytes=%d, bytesDownloaded=%d, errorCode=%d)",
                    getClass().getSimpleName(),
                    totalBytes,
                    bytesDownloaded,
                    errorCode
            );
        }
    }

    interface StatusListener {
        void onUpdate(@NonNull Status status);
    }
}

class ApkDownloadRequestImpl implements ApkDownloadRequest {
    private final Context context;
    private final Executor executor;
    private final Logger logger;
    private final LocallyDynamicConfigurationDto configuration;
    private final SplitInstallRequest splitInstallRequest;
    private final String title;
    private final String description;
    private final StatusListener statusListener;
    private final DownloadManager downloadManager;
    private final File downloadedApks;
    private final File downloadedSplitsDir;
    private final String filename = UUID.randomUUID().toString() + ".zip";
    private DownloadManager.Query query = null;
    @VisibleForTesting
    long downloadId = -1;

    ApkDownloadRequestImpl(
            @NonNull Context context,
            @NonNull Executor executor,
            @NonNull Logger logger,
            @NonNull LocallyDynamicConfigurationDto configuration,
            @NonNull SplitInstallRequest splitInstallRequest,
            @NonNull String title,
            @NonNull String description,
            @NonNull StatusListener statusListener,
            @NonNull DownloadManager downloadManager
    ) {
        this.context = context;
        this.executor = executor;
        this.logger = logger;
        this.configuration = configuration;
        this.splitInstallRequest = splitInstallRequest;
        this.title = title;
        this.description = description;
        this.statusListener = statusListener;
        this.downloadManager = downloadManager;
        this.downloadedApks = new File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath(),
                filename
        );
        this.downloadedSplitsDir = new File(
                context.getFilesDir()
                        + "/splitcompat/"
                        + configuration.getVersionCode()
                        + "/verified-splits"
        );
        if (!downloadedSplitsDir.exists()) {
            boolean created = downloadedSplitsDir.mkdirs();
            if (!created) {
                logger.e("Failed to create directory " + downloadedSplitsDir.getAbsolutePath());
            }
        }
    }

    private void schedulePoll(long delay, final @Nullable ApkDownloadRequest.Status prevStatus) {
        executor.schedule(delay, new Executor.Callbacks<ApkDownloadRequest.Status>() {
            @Override
            public ApkDownloadRequest.Status execute() {
                return pollDownloadStatus(prevStatus);
            }

            @Override
            public void onComplete(ApkDownloadRequest.Status status) {
                if (!(prevStatus instanceof Status.Running)
                        || !(status instanceof Status.Running)
                        || prevStatus.bytesDownloaded != status.bytesDownloaded) {
                    // Only notify if progress has been made
                    statusListener.onUpdate(status);
                }
                if (!(status instanceof ApkDownloadRequest.Status.Failed)
                        && !(status instanceof ApkDownloadRequest.Status.Successful)
                        && !(status instanceof ApkDownloadRequest.Status.Canceled)
                ) {
                    schedulePoll(20, status);
                }
            }

            @Override
            public void onError(Exception exception) {
                logger.e("Download failed", exception);
                statusListener.onUpdate(new ApkDownloadRequest.Status.Failed(0, 0, 0));
            }
        });
    }

    private ApkDownloadRequest.Status pollDownloadStatus(@Nullable ApkDownloadRequest.Status prevStatus) {
        Cursor cursor = downloadManager.query(query);
        Status status;
        if (cursor.moveToFirst()) {
            int statusCode = cursor.getInt(
                    cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            );
            long totalBytes = (long) cursor.getInt(
                    cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            );
            long bytesDownloaded = (long) cursor.getInt(
                    cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            );
            int errorCode = cursor.getInt(
                    cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
            );

            if (statusCode == DownloadManager.STATUS_FAILED) {
                downloadManager.remove(downloadId);
                status = new ApkDownloadRequest.Status.Failed(
                        totalBytes,
                        bytesDownloaded,
                        errorCode
                );
            } else if (statusCode == DownloadManager.STATUS_SUCCESSFUL) {
                if (downloadedApks.exists()) {
                    List<File> extractedApks = FileUtils.unzip(
                            downloadedApks,
                            downloadedSplitsDir.getAbsolutePath()
                    );

                    List<String> filePaths = new ArrayList<String>(extractedApks.size());
                    for (File extractedApk : extractedApks) {
                        filePaths.add(extractedApk.getAbsolutePath());
                    }

                    logger.i("Extracted the following APKS: " + ListUtils.toString(filePaths));

                    downloadManager.remove(downloadId);
                    status = new ApkDownloadRequest.Status.Successful(
                            totalBytes,
                            bytesDownloaded,
                            extractedApks
                    );
                } else {
                    downloadManager.remove(downloadId);
                    status = new ApkDownloadRequest.Status.Canceled(
                            totalBytes,
                            bytesDownloaded
                    );
                }
            } else if (statusCode == DownloadManager.STATUS_PENDING) {
                status = new ApkDownloadRequest.Status.Pending(
                        totalBytes,
                        bytesDownloaded
                );
            } else if (statusCode == DownloadManager.STATUS_RUNNING) {
                status = new ApkDownloadRequest.Status.Running(
                        totalBytes,
                        bytesDownloaded
                );
            } else if (statusCode == DownloadManager.STATUS_PAUSED) {
                status = new ApkDownloadRequest.Status.Paused(
                        totalBytes,
                        bytesDownloaded
                );
            } else {
                status = new ApkDownloadRequest.Status.Unknown(
                        totalBytes,
                        bytesDownloaded,
                        statusCode
                );
            }
        } else if (prevStatus != null) {
            status = new ApkDownloadRequest.Status.Canceled(
                    prevStatus.totalBytes,
                    prevStatus.bytesDownloaded
            );
        } else {
            status = new ApkDownloadRequest.Status.Unknown(0, 0, -1);
        }

        cursor.close();

        return status;
    }

    @VisibleForTesting
    DownloadManager.Request createRequest(@NonNull Uri uri) {
        return new DownloadManager.Request(uri);
    }

    @Override
    public void cancel() {
        downloadManager.remove(downloadId);
    }

    @Override
    public void start() {
        if (downloadId > -1) {
            return;
        }

        List<String> features = splitInstallRequest.getModuleNames();
        List<String> languages = new ArrayList<String>(splitInstallRequest.getLanguages().size());
        for (Locale locale : splitInstallRequest.getLanguages()) {
            languages.add(locale.getLanguage());
        }

        String featuresQueryParamValue = joinToString(features, ",");
        String languagesQueryParamValue = joinToString(languages, ",");

        Uri.Builder uriBuilder = Uri.parse(configuration.getServerUrl())
                .buildUpon()
                .appendPath("download")
                .appendQueryParameter("device-id", configuration.getDeviceId())
                .appendQueryParameter("variant", configuration.getVariantName())
                .appendQueryParameter("version", String.valueOf(configuration.getVersionCode()))
                .appendQueryParameter("application-id", configuration.getApplicationId());

        if (configuration.getThrottleDownloadBy() > 0) {
            uriBuilder.appendQueryParameter("throttle",
                    String.valueOf(configuration.getThrottleDownloadBy()));
        }

        if (!features.isEmpty()) {
            uriBuilder.appendQueryParameter("features", featuresQueryParamValue);
        }

        if (!languages.isEmpty()) {
            uriBuilder.appendQueryParameter("languages", languagesQueryParamValue);
        }

        Uri uri = uriBuilder.build();

        String authorization = StringUtils.toBase64(String.format(
                Locale.ENGLISH,
                "%s:%s",
                configuration.getUsername(),
                configuration.getPassword()
        ));

        DownloadManager.Request downloadRequest = createRequest(uri)
                .addRequestHeader("Authorization", "Basic " + authorization)
                .setTitle(title)
                .setDescription(description)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setAllowedOverRoaming(true)
                .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE | DownloadManager.Request.NETWORK_WIFI)
                .setMimeType("application/zip")
                .setDestinationInExternalFilesDir(
                        context,
                        Environment.DIRECTORY_DOWNLOADS,
                        filename
                );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            downloadRequest.setAllowedOverMetered(true);
        }

        long downloadId = downloadManager.enqueue(downloadRequest);
        this.downloadId = downloadId;

        logger.i("Started download from " + uri.toString());
        logger.i("Downloading to " + downloadedSplitsDir.getAbsolutePath());

        query = new DownloadManager.Query()
                .setFilterById(downloadId)
                .setFilterByStatus(
                        DownloadManager.STATUS_FAILED
                                | DownloadManager.STATUS_PAUSED
                                | DownloadManager.STATUS_SUCCESSFUL
                                | DownloadManager.STATUS_RUNNING
                                | DownloadManager.STATUS_PENDING
                );

        schedulePoll(0, null);
    }
}