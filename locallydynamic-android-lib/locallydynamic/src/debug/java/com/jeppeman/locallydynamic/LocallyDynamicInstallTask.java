package com.jeppeman.locallydynamic;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;

import com.google.android.play.core.splitcompat.SplitCompat;
import com.google.android.play.core.splitinstall.SplitInstallException;
import com.google.android.play.core.splitinstall.SplitInstallRequest;
import com.google.android.play.core.splitinstall.SplitInstallSessionState;
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener;
import com.google.android.play.core.splitinstall.model.SplitInstallErrorCode;
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus;
import com.google.android.play.core.tasks.OnCompleteListener;
import com.google.android.play.core.tasks.OnFailureListener;
import com.google.android.play.core.tasks.OnSuccessListener;
import com.google.android.play.core.tasks.RuntimeExecutionException;
import com.google.android.play.core.tasks.Task;
import com.jeppeman.locallydynamic.net.ListUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import static com.jeppeman.locallydynamic.serialization.StringUtils.joinToString;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
abstract class LocallyDynamicInstallTask extends Task<Integer> {
    abstract SplitInstallSessionState getCurrentState();

    abstract void cancel(int sessionId);

    abstract void start();

    static LocallyDynamicInstallTask create(
            @NonNull LocallyDynamicConfigurationRepository locallyDynamicConfigurationRepository,
            @NonNull Context context,
            @NonNull List<SplitInstallStateUpdatedListener> installListeners,
            @NonNull SplitInstallRequest splitInstallRequest,
            @NonNull Executor executor,
            @NonNull Logger logger,
            int id) {
        return new LocallyDynamicInstallTaskImpl(
                locallyDynamicConfigurationRepository,
                context,
                installListeners,
                splitInstallRequest,
                executor,
                logger,
                id);
    }
}

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class LocallyDynamicInstallTaskImpl extends LocallyDynamicInstallTask {
    private final LocallyDynamicConfigurationRepository locallyDynamicConfigurationRepository;
    private final Context context;
    private final List<SplitInstallStateUpdatedListener> installListeners;
    private final SplitInstallRequest splitInstallRequest;
    private final Executor executor;
    private final Logger logger;
    private final int id;
    private final List<String> moduleNames;
    private final List<String> languages;
    private final List<OnSuccessListener<? super Integer>> onSuccessListeners = new LinkedList<OnSuccessListener<? super Integer>>();
    private final List<OnCompleteListener<Integer>> onCompleteListeners = new LinkedList<OnCompleteListener<Integer>>();
    private final List<OnFailureListener> onFailureListeners = new LinkedList<OnFailureListener>();
    private Exception exception = null;
    private ApkDownloadRequest apkDownloadRequest = null;
    private SplitInstallSessionState currentState = null;

    LocallyDynamicInstallTaskImpl(
            @NonNull LocallyDynamicConfigurationRepository locallyDynamicConfigurationRepository,
            @NonNull Context context,
            @NonNull List<SplitInstallStateUpdatedListener> installListeners,
            @NonNull SplitInstallRequest splitInstallRequest,
            @NonNull Executor executor,
            @NonNull Logger logger,
            int id) {
        this.locallyDynamicConfigurationRepository = locallyDynamicConfigurationRepository;
        this.context = context;
        this.installListeners = installListeners;
        this.splitInstallRequest = splitInstallRequest;
        this.executor = executor;
        this.logger = logger;
        this.id = id;
        this.moduleNames = splitInstallRequest.getModuleNames();
        List<String> languages = new ArrayList<String>(splitInstallRequest.getLanguages().size());
        for (Locale locale : splitInstallRequest.getLanguages()) {
            languages.add(locale.getLanguage());
        }
        this.languages = languages;

        updateSessionState(SplitInstallSessionStatus.PENDING);
    }

    @Override
    void start() {
        logger.i("Starting installation of " +
                ListUtils.toString(moduleNames)
                + ", " + ListUtils.toString(languages));

        executor.execute(new Executor.Callbacks<Result<LocallyDynamicConfigurationDto>>() {
            @Override
            public Result<LocallyDynamicConfigurationDto> execute() {
                return locallyDynamicConfigurationRepository.getConfiguration();
            }

            @Override
            public void onComplete(Result<LocallyDynamicConfigurationDto> result) {
                result.doOnSuccess(new Result.Success.Callback<LocallyDynamicConfigurationDto>() {
                    @Override
                    public void success(LocallyDynamicConfigurationDto configuration) {
                        startDownload(configuration);
                    }
                }).doOnFailure(new Result.Failure.Callback() {
                    @Override
                    public void failure(@Nullable Exception exception) {
                        exception = exception != null
                                ? exception
                                : new SplitInstallException(SplitInstallErrorCode.NETWORK_ERROR);
                        notifyFailure(exception, SplitInstallErrorCode.NETWORK_ERROR);
                    }
                });
            }

            @Override
            public void onError(Exception exception) {
                notifyFailure(exception, SplitInstallErrorCode.INTERNAL_ERROR);
            }
        });
    }

    @VisibleForTesting
    ApkDownloadRequest createApkDownloadRequest(
            LocallyDynamicConfigurationDto configuration,
            ApkDownloadRequest.StatusListener statusListener) {
        StringBuilder title = new StringBuilder("Downloading");
        StringBuilder description = new StringBuilder();
        if (!splitInstallRequest.getModuleNames().isEmpty()) {
            title.append(" features");
            description.append(joinToString(splitInstallRequest.getModuleNames(), ", "));
        }
        if (!splitInstallRequest.getLanguages().isEmpty()) {
            title.append(splitInstallRequest.getModuleNames().isEmpty()
                    ? " languages"
                    : " and languages");
            description.append("\n");
            for (int i = 0; i < splitInstallRequest.getLanguages().size(); i++) {
                Locale locale = splitInstallRequest.getLanguages().get(i);
                description.append(locale.getLanguage());
                if (i < splitInstallRequest.getLanguages().size() - 1) {
                    description.append(", ");
                }
            }
        }
        title.append("...");
        return ApkDownloadRequestFactory.create(
                context,
                executor,
                logger,
                configuration,
                splitInstallRequest,
                title.toString(),
                description.toString(),
                statusListener
        );
    }

    @VisibleForTesting
    ApkInstaller createApkInstaller(ApkInstaller.StatusListener statusListener) {
        return ApkInstallerFactory.create(
                context,
                logger,
                statusListener);
    }

    private void startDownload(LocallyDynamicConfigurationDto configuration) {
        apkDownloadRequest = createApkDownloadRequest(
                configuration,
                new ApkDownloadRequest.StatusListener() {
                    @Override
                    public void onUpdate(@NonNull ApkDownloadRequest.Status status) {
                        logger.i("Downloading, current status: " + status);
                        if (status instanceof ApkDownloadRequest.Status.Successful) {
                            handleDownloadSuccessful((ApkDownloadRequest.Status.Successful) status);
                        } else if (status instanceof ApkDownloadRequest.Status.Pending) {
                            handleDownloadPending((ApkDownloadRequest.Status.Pending) status);
                        } else if (status instanceof ApkDownloadRequest.Status.Running) {
                            handleDownloadRunning((ApkDownloadRequest.Status.Running) status);
                        } else if (status instanceof ApkDownloadRequest.Status.Unknown) {
                            handleUnknown((ApkDownloadRequest.Status.Unknown) status);
                        } else if (status instanceof ApkDownloadRequest.Status.Paused) {
                            handleDownloadPaused((ApkDownloadRequest.Status.Paused) status);
                        } else if (status instanceof ApkDownloadRequest.Status.Failed) {
                            handleDownloadFailure((ApkDownloadRequest.Status.Failed) status);
                        } else if (status instanceof ApkDownloadRequest.Status.Canceled) {
                            handleDownloadCancelled((ApkDownloadRequest.Status.Canceled) status);
                        }
                    }
                });
        apkDownloadRequest.start();
    }

    private void handleDownloadPending(ApkDownloadRequest.Status.Pending status) {
        notifyInstallListeners(status);
    }

    private void handleDownloadRunning(ApkDownloadRequest.Status.Running status) {
        notifyInstallListeners(status);
    }

    private void handleDownloadPaused(ApkDownloadRequest.Status.Paused status) {
        notifyInstallListeners(status);
    }

    private void handleUnknown(ApkDownloadRequest.Status.Unknown status) {
        notifyInstallListeners(status);
    }

    private void handleDownloadCancelled(ApkDownloadRequest.Status.Canceled status) {
        notifyInstallListeners(status);
    }

    private void handleDownloadFailure(ApkDownloadRequest.Status.Failed status) {
        notifyFailure(
                new Exception("Failed to download: " + status.errorCode),
                statusCode(status)
        );
    }

    private void handleDownloadSuccessful(final ApkDownloadRequest.Status.Successful status) {
        notifyInstallListeners(status);

        ApkInstaller apkInstaller = createApkInstaller(new ApkInstaller.StatusListener() {
            @Override
            public void onUpdate(ApkInstaller.Status installerStatus) {
                logger.i("Installing, current status: " + installerStatus);
                if (installerStatus instanceof ApkInstaller.Status.Installing) {
                    handleInstalling((ApkInstaller.Status.Installing) installerStatus);
                } else if (installerStatus instanceof ApkInstaller.Status.Installed) {
                    handleInstallSuccessful((ApkInstaller.Status.Installed) installerStatus);
                } else if (installerStatus instanceof ApkInstaller.Status.Pending) {
                    handleInstallPending((ApkInstaller.Status.Pending) installerStatus);
                } else if (installerStatus instanceof ApkInstaller.Status.Failed) {
                    handleInstallFailure((ApkInstaller.Status.Failed) installerStatus);
                }
            }
        });

        apkInstaller.install(status.apks);
    }

    private void handleInstalling(ApkInstaller.Status.Installing status) {
        notifyInstallListeners(status);
    }

    private void handleInstallPending(ApkInstaller.Status.Pending status) {
        notifyInstallListeners(status);
    }

    private void handleInstallSuccessful(final ApkInstaller.Status.Installed installerResult) {
        executor.execute(new Executor.Callbacks<Boolean>() {
            @Override
            public Boolean execute() {
                return SplitCompat.a(context);
            }

            @Override
            public void onComplete(Boolean result) {
                String resultString = result
                        ? "success"
                        : "failure";
                logger.i("SplitCompat install result: " + resultString);
                notifyInstalled(installerResult);
            }

            @Override
            public void onError(Exception exception) {
                notifyFailure(exception, SplitInstallErrorCode.INTERNAL_ERROR);
            }
        });
    }

    private void handleInstallFailure(ApkInstaller.Status.Failed status) {
        notifyFailure(status.exception, status.code);
    }

    private int statusCode(ApkDownloadRequest.Status status) {
        if (status instanceof ApkDownloadRequest.Status.Pending) {
            return SplitInstallSessionStatus.PENDING;
        }
        if (status instanceof ApkDownloadRequest.Status.Successful) {
            return SplitInstallSessionStatus.DOWNLOADED;
        }
        if (status instanceof ApkDownloadRequest.Status.Unknown) {
            return SplitInstallSessionStatus.UNKNOWN;
        }
        if (status instanceof ApkDownloadRequest.Status.Failed) {
            return SplitInstallSessionStatus.FAILED;
        }
        if (status instanceof ApkDownloadRequest.Status.Paused) {
            return SplitInstallSessionStatus.PENDING;
        }
        if (status instanceof ApkDownloadRequest.Status.Running) {
            return SplitInstallSessionStatus.DOWNLOADING;
        }
        if (status instanceof ApkDownloadRequest.Status.Canceled) {
            return SplitInstallSessionStatus.CANCELED;
        }

        return SplitInstallSessionStatus.FAILED;
    }

    private int statusCode(ApkInstaller.Status status) {
        if (status instanceof ApkInstaller.Status.Installing) {
            return SplitInstallSessionStatus.INSTALLING;
        }
        if (status instanceof ApkInstaller.Status.Installed) {
            return SplitInstallSessionStatus.INSTALLED;
        }
        if (status instanceof ApkInstaller.Status.Failed) {
            return SplitInstallSessionStatus.FAILED;
        }
        if (status instanceof ApkInstaller.Status.Pending) {
            return SplitInstallSessionStatus.PENDING;
        }

        return SplitInstallSessionStatus.FAILED;
    }

    private void notifyInstallListeners(ApkDownloadRequest.Status status) {
        updateSessionState(
                statusCode(status),
                status.bytesDownloaded,
                status.totalBytes,
                SplitInstallErrorCode.NETWORK_ERROR
        );
    }

    private void notifyInstallListeners(ApkInstaller.Status status) {
        updateSessionState(statusCode(status), SplitInstallErrorCode.NETWORK_ERROR);
    }

    private void updateSessionState(int status, int errorCode) {
        updateSessionState(
                status,
                currentState != null ? currentState.bytesDownloaded() : 0,
                currentState != null ? currentState.totalBytesToDownload() : 0,
                errorCode
        );
    }

    private void updateSessionState(int status) {
        updateSessionState(status, -1);
    }

    private void updateSessionState(
            int status,
            long bytesDownloaded,
            long totalBytes,
            int errorCode) {
        SplitInstallSessionState newState = SplitInstallSessionState.create(
                id,
                status,
                errorCode,
                bytesDownloaded,
                totalBytes,
                moduleNames,
                languages
        );

        currentState = newState;

        for (SplitInstallStateUpdatedListener listener : installListeners) {
            if (listener != null) {
                listener.onStateUpdate(newState);
            }
        }
    }

    private void notifyInstalled(ApkInstaller.Status.Installed status) {
        notifyInstallListeners(status);

        notifySuccess();
    }

    private void notifyCancelled() {
        logger.i("Install canceled");
        updateSessionState(SplitInstallSessionStatus.CANCELED);

        notifyCompleted();
    }

    @VisibleForTesting
    void notifyCompleted() {
        logger.i("Install completed");
        executor.executeForeground(new Executor.Callbacks<Void>() {
            @Override
            Void execute() {
                for (OnCompleteListener<Integer> onCompleteListener : onCompleteListeners) {
                    if (onCompleteListener != null) {
                        onCompleteListener.onComplete(LocallyDynamicInstallTaskImpl.this);
                    }
                }

                return null;
            }
        });
    }

    private void notifyFailure(final Exception exception, int code) {
        this.exception = exception;
        logger.e("Install failed", exception);
        updateSessionState(SplitInstallSessionStatus.FAILED, code);
        notifyCompleted();
        executor.executeForeground(new Executor.Callbacks<Void>() {
            @Override
            Void execute() {
                for (OnFailureListener onFailureListener : onFailureListeners) {
                    if (onFailureListener != null) {
                        onFailureListener.onFailure(exception);
                    }
                }

                return null;
            }
        });
    }

    private void notifySuccess() {
        logger.i("Install successful");
        notifyCompleted();
        executor.executeForeground(new Executor.Callbacks<Void>() {
            @Override
            Void execute() {
                for (OnSuccessListener<? super Integer> onSuccessListener : onSuccessListeners) {
                    if (onSuccessListener != null) {
                        onSuccessListener.onSuccess(getResult());
                    }
                }

                return null;
            }
        });
    }

    @Override
    public SplitInstallSessionState getCurrentState() {
        return currentState;
    }

    @Override
    public void cancel(int sessionId) {
        if (currentState.status() == SplitInstallSessionStatus.DOWNLOADING
                && apkDownloadRequest != null) {
            apkDownloadRequest.cancel();
        }
        notifyCancelled();
    }

    @Override
    public boolean isComplete() {
        return currentState.status() == SplitInstallSessionStatus.INSTALLED
                || currentState.status() == SplitInstallSessionStatus.CANCELED
                || currentState.status() == SplitInstallSessionStatus.FAILED;
    }

    @Override
    public boolean isSuccessful() {
        return currentState.status() == SplitInstallSessionStatus.INSTALLED;
    }


    @Override
    public Exception getException() {
        return exception;
    }

    @Override
    public Integer getResult() {
        if (currentState.status() == SplitInstallSessionStatus.INSTALLED
                || currentState.status() == SplitInstallSessionStatus.CANCELED) {
            return currentState.status();
        }
        if (currentState.status() == SplitInstallSessionStatus.FAILED) {
            throw new RuntimeExecutionException(exception);
        }

        throw new IllegalStateException("Task not yet completed, status: " + currentState.status());
    }

    @Override
    public <X extends Throwable> Integer getResult(Class<X> throwableType) throws X {
        if (currentState.status() == SplitInstallSessionStatus.INSTALLED
                || currentState.status() == SplitInstallSessionStatus.CANCELED) {
            return currentState.status();
        }
        if (currentState.status() == SplitInstallSessionStatus.FAILED) {
            throw new RuntimeExecutionException(exception);
        }
        if (exception != null
                && throwableType != null
                && exception.getClass() == throwableType) {
            throw throwableType.cast(exception);
        }

        throw new IllegalStateException("Task not yet completed, status: " + currentState.status());
    }

    @Override
    public Task<Integer> addOnFailureListener(OnFailureListener listener) {
        if (isComplete() && !isSuccessful()) {
            listener.onFailure(exception);
        } else {
            onFailureListeners.add(listener);
        }

        return this;
    }

    @Override
    public Task<Integer> addOnFailureListener(
            final java.util.concurrent.Executor executor,
            final OnFailureListener listener
    ) {
        if (isComplete() && !isSuccessful()) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    listener.onFailure(exception);
                }
            });
        } else {
            onFailureListeners.add(new OnFailureListener() {
                @Override
                public void onFailure(Exception e) {
                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            listener.onFailure(exception);
                        }
                    });
                }
            });
        }

        return this;
    }

    @Override
    public Task<Integer> addOnSuccessListener(OnSuccessListener<? super Integer> listener) {
        if (isSuccessful()) {
            listener.onSuccess(getResult());
        } else {
            onSuccessListeners.add(listener);
        }

        return this;
    }

    @Override
    public Task<Integer> addOnSuccessListener(
            final java.util.concurrent.Executor executor,
            final OnSuccessListener<? super Integer> listener) {
        if (isSuccessful()) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    listener.onSuccess(getResult());
                }
            });
        } else {
            onSuccessListeners.add(new OnSuccessListener<Integer>() {
                @Override
                public void onSuccess(Integer result) {
                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            listener.onSuccess(getResult());
                        }
                    });

                }
            });
        }

        return this;
    }

    @Override
    public Task<Integer> addOnCompleteListener(OnCompleteListener<Integer> listener) {
        if (isComplete()) {
            listener.onComplete(this);
        } else {
            onCompleteListeners.add(listener);
        }

        return this;
    }

    @Override
    public Task<Integer> addOnCompleteListener(
            final java.util.concurrent.Executor executor,
            final OnCompleteListener<Integer> listener) {
        if (isComplete()) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    listener.onComplete(LocallyDynamicInstallTaskImpl.this);
                }
            });
        } else {
            onCompleteListeners.add(new OnCompleteListener<Integer>() {
                @Override
                public void onComplete(final Task<Integer> task) {
                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            listener.onComplete(task);
                        }
                    });
                }
            });
        }

        return this;
    }
}