package com.jeppeman.globallydynamic.globalsplitinstall;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.os.ResultReceiver;

import com.jeppeman.globallydynamic.net.HttpClient;
import com.jeppeman.globallydynamic.net.HttpMethod;
import com.jeppeman.globallydynamic.net.HttpUrl;
import com.jeppeman.globallydynamic.net.ListUtils;
import com.jeppeman.globallydynamic.net.Request;
import com.jeppeman.globallydynamic.net.Response;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;

class ApkDownloadRequestFactory {
    static ApkDownloadRequest create(
            @NonNull Context context,
            @NonNull Executor executor,
            @NonNull Logger logger,
            @NonNull SignatureProvider signatureProvider,
            @NonNull GloballyDynamicConfigurationDto configuration,
            @NonNull GlobalSplitInstallRequestInternal splitInstallRequest,
            @NonNull HttpClient httpClient,
            @NonNull ApkDownloadRequest.StatusListener statusListener
    ) {
        return new ApkDownloadRequestImpl(
                context,
                executor,
                logger,
                signatureProvider,
                configuration,
                splitInstallRequest,
                statusListener,
                httpClient
        );
    }
}

interface ApkDownloadRequest extends Parcelable {
    void start();

    void cancel();

    abstract class Status {
        final long totalBytes;
        final long bytesDownloaded;
        @GlobalSplitInstallErrorCode
        final int errorCode;

        Status(long totalBytes, long bytesDownloaded) {
            this(totalBytes, bytesDownloaded, GlobalSplitInstallErrorCode.NO_ERROR);
        }

        Status(long totalBytes, long bytesDownloaded, @GlobalSplitInstallErrorCode int errorCode) {
            this.totalBytes = totalBytes;
            this.bytesDownloaded = bytesDownloaded;
            this.errorCode = errorCode;
        }

        static class Enqueued extends Status {
            final int id;

            Enqueued(int id) {
                super(0, 0);
                this.id = id;
            }
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
            Failed(long totalBytes, long bytesDownloaded, @GlobalSplitInstallErrorCode int code) {
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

class ApkDownloadRequestImpl extends
        ResultReceiver implements
        ApkDownloadRequest,
        HttpClient.DownloadCallbacks<String> {
    private final Executor executor;
    private final Logger logger;
    private final SignatureProvider signatureProvider;
    private final GloballyDynamicConfigurationDto configuration;
    private final GlobalSplitInstallRequestInternal splitInstallRequest;
    private final StatusListener statusListener;
    private final HttpClient httpClient;
    private final String filename = UUID.randomUUID().toString() + ".zip";
    @VisibleForTesting
    final File downloadedApks;
    @VisibleForTesting
    final File downloadedSplitsDir;
    @VisibleForTesting
    int downloadId = -1;

    ApkDownloadRequestImpl(
            @NonNull Context context,
            @NonNull Executor executor,
            @NonNull Logger logger,
            @NonNull SignatureProvider signatureProvider,
            @NonNull GloballyDynamicConfigurationDto configuration,
            @NonNull GlobalSplitInstallRequestInternal splitInstallRequest,
            @NonNull StatusListener statusListener,
            @NonNull HttpClient httpClient
    ) {
        super(executor.getForegroundHandler());
        this.executor = executor;
        this.logger = logger;
        this.signatureProvider = signatureProvider;
        this.configuration = configuration;
        this.splitInstallRequest = splitInstallRequest;
        this.statusListener = statusListener;
        this.httpClient = httpClient;
        this.downloadedApks = new File(
                context.getCacheDir(),
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

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        boolean canceled = resultData.getBoolean(InstallService.ACTION_CANCELED, false);
        if (canceled) {
            cancel();
        }
    }

    @Override
    public Handler getCallbackHandler() {
        return executor.getForegroundHandler();
    }

    @Override
    public void onResponse(Response<String> response, final long bytesDownloaded, final long totalBytesToDownload) {
        if (response.isSuccessful()) {
            executor.execute(new Executor.Callbacks<List<File>>() {
                @Override
                List<File> execute() {
                    List<File> extractedApks = FileUtils.unzip(
                            downloadedApks,
                            downloadedSplitsDir.getAbsolutePath()
                    );

                    List<String> filePaths = new ArrayList<String>(extractedApks.size());
                    for (File extractedApk : extractedApks) {
                        filePaths.add(extractedApk.getAbsolutePath());
                    }

                    logger.i("Extracted the following APKS: " + ListUtils.toString(filePaths));

                    downloadedApks.delete();

                    return extractedApks;
                }

                @Override
                void onComplete(List<File> result) {
                    statusListener.onUpdate(new Status.Successful(totalBytesToDownload, bytesDownloaded, result));
                }

                @Override
                void onError(Exception exception) {
                    logger.e("Failed to finalize downloaded splits", exception);
                    statusListener.onUpdate(new Status.Failed(0, 0, GlobalSplitInstallErrorCode.INTERNAL_ERROR));
                }
            });
        } else {
            logger.e("Failed to download splits", new HttpException(response.getCode(), response.getErrorBody()));
            statusListener.onUpdate(new Status.Failed(0, 0, GlobalSplitInstallErrorCode.NETWORK_ERROR));
        }
    }

    @Override
    public void onStartDownload(long totalBytes) {
        statusListener.onUpdate(new Status.Enqueued(downloadId));
    }

    @Override
    public void onProgress(long bytesDownloaded, long totalBytesToDownload) {
        statusListener.onUpdate(new Status.Running(totalBytesToDownload, bytesDownloaded));
    }

    @Override
    public void onFailure(Throwable throwable) {
        logger.e("Failed to download splits", (Exception) throwable);
        statusListener.onUpdate(new Status.Failed(0, 0, GlobalSplitInstallErrorCode.NETWORK_ERROR));
    }

    @Override
    public void onCanceled() {
        statusListener.onUpdate(new Status.Canceled(0, 0));
    }

    @Override
    public void cancel() {
        httpClient.cancelDownload(downloadId);
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

        String featuresQueryParamValue = com.jeppeman.globallydynamic.serialization.StringUtils.joinToString(features, ",");
        String languagesQueryParamValue = com.jeppeman.globallydynamic.serialization.StringUtils.joinToString(languages, ",");

        HttpUrl.Builder uriBuilder = HttpUrl.parse(configuration.getServerUrl())
                .newBuilder()
                .pathSegments("download")
                .queryParam("variant", configuration.getVariantName())
                .queryParam("version", String.valueOf(configuration.getVersionCode()))
                .queryParam("signature", signatureProvider.getCertificateFingerprint())
                .queryParam("application-id", configuration.getApplicationId());

        if (configuration.getThrottleDownloadBy() > 0) {
            uriBuilder.queryParam("throttle",
                    String.valueOf(configuration.getThrottleDownloadBy()));
        }

        if (splitInstallRequest.shouldIncludeMissingSplits()) {
            uriBuilder.queryParam("include-missing",
                    String.valueOf(splitInstallRequest.shouldIncludeMissingSplits()));
        }

        if (!features.isEmpty()) {
            uriBuilder.queryParam("features", featuresQueryParamValue);
        }

        if (!languages.isEmpty()) {
            uriBuilder.queryParam("languages", languagesQueryParamValue);
        }

        HttpUrl uri = uriBuilder.build();

        logger.i("Started download from " + uri.toString());
        logger.i("Downloading to " + downloadedSplitsDir.getAbsolutePath());

        final Request request = Request.builder()
                .setBody(configuration.getDeviceSpec())
                .setMethod(HttpMethod.POST)
                .url(uri)
                .build();

        downloadId = httpClient.downloadFile(request, downloadedApks, this);
    }
}