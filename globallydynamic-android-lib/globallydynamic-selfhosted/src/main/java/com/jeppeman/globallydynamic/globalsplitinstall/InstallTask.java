package com.jeppeman.globallydynamic.globalsplitinstall;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;

import com.jeppeman.globallydynamic.net.HttpClient;
import com.jeppeman.globallydynamic.net.ListUtils;
import com.jeppeman.globallydynamic.tasks.GlobalSplitInstallTask;
import com.jeppeman.globallydynamic.tasks.OnGlobalSplitInstallCompleteListener;
import com.jeppeman.globallydynamic.tasks.OnGlobalSplitInstallFailureListener;
import com.jeppeman.globallydynamic.tasks.OnGlobalSplitInstallSuccessListener;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
abstract class InstallTask implements GlobalSplitInstallTask<Integer> {
    @Nullable
    abstract ApkInstaller getInstaller();

    @Nullable
    abstract ApkDownloadRequest getDownloadRequest();

    abstract GlobalSplitInstallRequestInternal getInstallRequest();

    abstract GlobalSplitInstallSessionState getCurrentState();

    abstract void cancel(int sessionId);

    abstract void start();

    abstract void registerStateListener(GlobalSplitInstallUpdatedListener listener);
}

interface InstallListenersProvider {
    List<GlobalSplitInstallUpdatedListener> getListeners();
}

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class InstallTaskImpl
        extends InstallTask
        implements ServiceConnection {
    private final GloballyDynamicConfigurationRepository globallyDynamicConfigurationRepository;
    private final Context context;
    private final InstallListenersProvider installListenersProvider;
    private final GlobalSplitInstallRequestInternal splitInstallRequest;
    private final Executor executor;
    private final Logger logger;
    private final SignatureProvider signatureProvider;
    private final ApplicationPatcher applicationPatcher;
    private final HttpClient httpClient;
    private final TaskRegistry taskRegistry;
    private final List<String> moduleNames;
    private final List<String> languages;
    private final List<GlobalSplitInstallUpdatedListener> localInstallListeners =
            new LinkedList<GlobalSplitInstallUpdatedListener>();
    private final List<OnGlobalSplitInstallSuccessListener<? super Integer>> onSuccessListeners =
            new LinkedList<OnGlobalSplitInstallSuccessListener<? super Integer>>();
    private final List<OnGlobalSplitInstallCompleteListener<Integer>> onCompleteListeners =
            new LinkedList<OnGlobalSplitInstallCompleteListener<Integer>>();
    private final List<OnGlobalSplitInstallFailureListener> onFailureListeners =
            new LinkedList<OnGlobalSplitInstallFailureListener>();
    private Exception exception = null;
    private ApkDownloadRequest apkDownloadRequest = null;
    private ApkInstaller apkInstaller = null;
    private GlobalSplitInstallSessionState currentState = null;
    private boolean isBoundToService;
    private int sessionId = -1;

    InstallTaskImpl(
            @NonNull GloballyDynamicConfigurationRepository globallyDynamicConfigurationRepository,
            @NonNull Context context,
            @NonNull InstallListenersProvider installListenersProvider,
            @NonNull GlobalSplitInstallRequestInternal splitInstallRequest,
            @NonNull Executor executor,
            @NonNull Logger logger,
            @NonNull SignatureProvider signatureProvider,
            @NonNull ApplicationPatcher applicationPatcher,
            @NonNull TaskRegistry taskRegistry,
            @NonNull HttpClient httpClient
    ) {
        this.globallyDynamicConfigurationRepository = globallyDynamicConfigurationRepository;
        this.context = context;
        this.installListenersProvider = installListenersProvider;
        this.splitInstallRequest = splitInstallRequest;
        this.executor = executor;
        this.logger = logger;
        this.signatureProvider = signatureProvider;
        this.applicationPatcher = applicationPatcher;
        this.taskRegistry = taskRegistry;
        this.httpClient = httpClient;
        this.moduleNames = splitInstallRequest.getModuleNames();
        List<String> languages = new ArrayList<String>(splitInstallRequest.getLanguages().size());
        for (Locale locale : splitInstallRequest.getLanguages()) {
            languages.add(locale.getLanguage());
        }
        this.languages = languages;

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q
                && !context.getPackageManager().canRequestPackageInstalls()) {
            sessionId = httpClient.getNextDownloadId();
            notifySuccess();
            updateSessionState(
                    GlobalSplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION,
                    GlobalSplitInstallErrorCode.NO_ERROR
            );
        } else {
            startInstallService();
        }
    }

    private void startInstallService() {
        InstallService.start(context);
        context.bindService(InstallService.getIntent(context), this, Context.BIND_AUTO_CREATE);
    }

    private List<GlobalSplitInstallUpdatedListener> getListeners() {
        List<GlobalSplitInstallUpdatedListener> listeners = new LinkedList<GlobalSplitInstallUpdatedListener>();
        listeners.addAll(localInstallListeners);
        listeners.addAll(installListenersProvider.getListeners());
        return listeners;
    }

    @Override
    void start() {
        logger.i("Starting installation of " +
                ListUtils.toString(moduleNames)
                + ", " + ListUtils.toString(languages));

        executor.execute(new Executor.Callbacks<Result<GloballyDynamicConfigurationDto>>() {
            @Override
            public Result<GloballyDynamicConfigurationDto> execute() {
                return globallyDynamicConfigurationRepository.getConfiguration();
            }

            @Override
            public void onComplete(Result<GloballyDynamicConfigurationDto> result) {
                result.doOnSuccess(new Result.Success.Callback<GloballyDynamicConfigurationDto>() {
                    @Override
                    public void success(GloballyDynamicConfigurationDto configuration) {
                        if (splitInstallRequest.isUninstall()) {
                            startUninstall();
                        } else {
                            startDownload(configuration);
                        }
                    }
                }).doOnFailure(new Result.Failure.Callback() {
                    @Override
                    public void failure(@Nullable Exception exception) {
                        exception = exception != null
                                ? exception
                                : new RuntimeException();
                        notifyFailure(exception, GlobalSplitInstallErrorCode.INTERNAL_ERROR);
                    }
                });
            }

            @Override
            public void onError(Exception exception) {
                notifyFailure(exception, GlobalSplitInstallErrorCode.INTERNAL_ERROR);
            }
        });
    }

    @VisibleForTesting
    ApkDownloadRequest createApkDownloadRequest(
            GloballyDynamicConfigurationDto configuration,
            ApkDownloadRequest.StatusListener statusListener) {
        return ApkDownloadRequestFactory.create(
                context,
                executor,
                logger,
                signatureProvider,
                configuration,
                splitInstallRequest,
                httpClient,
                statusListener
        );
    }

    @VisibleForTesting
    ApkInstaller createApkInstaller(ApkInstaller.StatusListener statusListener) {
        apkInstaller = ApkInstallerFactory.create(
                context,
                logger,
                executor,
                applicationPatcher,
                statusListener
        );
        return apkInstaller;
    }

    private void startDownload(GloballyDynamicConfigurationDto configuration) {
        apkDownloadRequest = createApkDownloadRequest(
                configuration,
                new ApkDownloadRequest.StatusListener() {
                    @Override
                    public void onUpdate(@NonNull ApkDownloadRequest.Status status) {
                        if (status instanceof ApkDownloadRequest.Status.Successful) {
                            handleDownloadSuccessful((ApkDownloadRequest.Status.Successful) status);
                        } else if (status instanceof ApkDownloadRequest.Status.Enqueued) {
                            handleDownloadEnqueued((ApkDownloadRequest.Status.Enqueued) status);
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
                new RuntimeException("Failed to download splits: " + status.errorCode),
                GlobalSplitInstallErrorCode.INTERNAL_ERROR
        );
    }

    private void handleDownloadEnqueued(ApkDownloadRequest.Status.Enqueued status) {
        sessionId = status.id;
        notifyInstallListeners(status);
        notifySuccess();
    }

    private void startUninstall() {
        ApkInstaller apkInstaller = createApkInstaller(new ApkInstaller.StatusListener() {
            @Override
            public void onUpdate(ApkInstaller.Status installerStatus) {
                if (installerStatus instanceof ApkInstaller.Status.Installing) {
                    handleInstalling((ApkInstaller.Status.Installing) installerStatus);
                } else if (installerStatus instanceof ApkInstaller.Status.Installed) {
                    handleInstallSuccessful((ApkInstaller.Status.Installed) installerStatus);
                }if (installerStatus instanceof ApkInstaller.Status.Uninstalling) {
                    handleUninstalling((ApkInstaller.Status.Uninstalling) installerStatus);
                } else if (installerStatus instanceof ApkInstaller.Status.Uninstalled) {
                    handleUninstalled((ApkInstaller.Status.Uninstalled) installerStatus);
                } else if (installerStatus instanceof ApkInstaller.Status.Pending) {
                    handleInstallPending((ApkInstaller.Status.Pending) installerStatus);
                } else if (installerStatus instanceof ApkInstaller.Status.Failed) {
                    handleInstallFailure((ApkInstaller.Status.Failed) installerStatus);
                } else if (installerStatus instanceof ApkInstaller.Status.RequiresUserPermission) {
                    handleRequiresUserPermission((ApkInstaller.Status.RequiresUserPermission) installerStatus);
                } else if (installerStatus instanceof ApkInstaller.Status.Canceled) {
                    handleInstallCanceled((ApkInstaller.Status.Canceled) installerStatus);
                }
            }
        });

        sessionId = httpClient.getNextDownloadId();

        notifySuccess();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            apkInstaller.uninstall(splitInstallRequest.getModuleNames());
        } else {
            notifyFailure(new GlobalSplitInstallException(
                    GlobalSplitInstallErrorCode.INTERNAL_ERROR,
                    "Uninstalling requires API level >= 24 (N)"
            ), GlobalSplitInstallErrorCode.INTERNAL_ERROR);
        }
    }

    private void handleDownloadSuccessful(final ApkDownloadRequest.Status.Successful status) {
        notifyInstallListeners(status);

        ApkInstaller apkInstaller = createApkInstaller(new ApkInstaller.StatusListener() {
            @Override
            public void onUpdate(ApkInstaller.Status installerStatus) {
                if (installerStatus instanceof ApkInstaller.Status.Installing) {
                    handleInstalling((ApkInstaller.Status.Installing) installerStatus);
                } else if (installerStatus instanceof ApkInstaller.Status.Installed) {
                    handleInstallSuccessful((ApkInstaller.Status.Installed) installerStatus);
                }if (installerStatus instanceof ApkInstaller.Status.Uninstalling) {
                    handleUninstalling((ApkInstaller.Status.Uninstalling) installerStatus);
                } else if (installerStatus instanceof ApkInstaller.Status.Uninstalled) {
                    handleUninstalled((ApkInstaller.Status.Uninstalled) installerStatus);
                } else if (installerStatus instanceof ApkInstaller.Status.Pending) {
                    handleInstallPending((ApkInstaller.Status.Pending) installerStatus);
                } else if (installerStatus instanceof ApkInstaller.Status.Failed) {
                    handleInstallFailure((ApkInstaller.Status.Failed) installerStatus);
                } else if (installerStatus instanceof ApkInstaller.Status.RequiresUserPermission) {
                    handleRequiresUserPermission((ApkInstaller.Status.RequiresUserPermission) installerStatus);
                } else if (installerStatus instanceof ApkInstaller.Status.Canceled) {
                    handleInstallCanceled((ApkInstaller.Status.Canceled) installerStatus);
                }
            }
        });

        apkInstaller.install(status.apks);
    }

    private void handleInstallCanceled(ApkInstaller.Status.Canceled status) {
        notifyInstallListeners(status);
    }

    private void handleRequiresUserPermission(ApkInstaller.Status.RequiresUserPermission status) {
        notifyInstallListeners(status);
    }

    private void handleInstalling(ApkInstaller.Status.Installing status) {
        notifyInstallListeners(status);
    }

    private void handleUninstalling(ApkInstaller.Status.Uninstalling status) {
        notifyInstallListeners(status);
    }

    private void handleUninstalled(ApkInstaller.Status.Uninstalled status) {
        notifyInstallListeners(status);
    }

    private void handleInstallPending(ApkInstaller.Status.Pending status) {
        notifyInstallListeners(status);
    }

    private void handleInstallSuccessful(final ApkInstaller.Status.Installed installerResult) {
        notifyInstalled(installerResult);
    }

    private void handleInstallFailure(ApkInstaller.Status.Failed status) {
        notifyFailure(status.exception, status.code);
    }

    private void notifyInstallListeners(ApkDownloadRequest.Status status) {
        updateSessionState(
                GlobalSplitInstallSessionStatusMapper.fromDownloadStatus(status),
                status.bytesDownloaded,
                status.totalBytes,
                status instanceof ApkDownloadRequest.Status.Failed
                        ? GlobalSplitInstallErrorCode.NETWORK_ERROR
                        : GlobalSplitInstallErrorCode.NO_ERROR
        );
    }

    private void notifyInstallListeners(ApkInstaller.Status status) {
        updateSessionState(
                GlobalSplitInstallSessionStatusMapper.fromApkInstallerStatus(status),
                status instanceof ApkInstaller.Status.Failed
                        ? GlobalSplitInstallErrorCode.INTERNAL_ERROR
                        : GlobalSplitInstallErrorCode.NO_ERROR
        );
    }

    private void updateSessionState(int status, int errorCode) {
        updateSessionState(
                status,
                currentState != null ? currentState.bytesDownloaded() : 0,
                currentState != null ? currentState.totalBytesToDownload() : 0,
                errorCode
        );
    }

    private void updateSessionState(
            int status,
            long bytesDownloaded,
            long totalBytes,
            int errorCode) {
        final GlobalSplitInstallSessionState newState = GlobalSplitInstallSessionStateFactory.create(
                sessionId,
                status,
                errorCode,
                bytesDownloaded,
                totalBytes,
                moduleNames,
                languages
        );

        logger.i(GlobalSplitInstallSessionStateHelper.toString(currentState)
                + " -> "
                + GlobalSplitInstallSessionStateHelper.toString(newState));

        currentState = newState;

        for (final GlobalSplitInstallUpdatedListener listener : getListeners()) {
            if (listener != null) {
                executor.executeForeground(new Executor.Callbacks<Void>() {
                    @Override
                    Void execute() {
                        listener.onStateUpdate(newState);
                        return null;
                    }
                });
            }
        }

        if (status == GlobalSplitInstallSessionStatus.FAILED
                || status == GlobalSplitInstallSessionStatus.INSTALLED
                || status == GlobalSplitInstallSessionStatus.UNINSTALLED
                || status == GlobalSplitInstallSessionStatus.CANCELED) {
            if (isBoundToService) {
                try {
                    context.unbindService(this);
                } catch (Exception exception) {
                    logger.e("Failed to unbind service, this sould not happen", exception);
                }
            }
            taskRegistry.unregisterTask(this);
        }
    }

    private void notifyInstalled(ApkInstaller.Status.Installed status) {
        notifyInstallListeners(status);
    }

    @VisibleForTesting
    void notifyCompleted() {
        executor.executeForeground(new Executor.Callbacks<Void>() {
            @Override
            Void execute() {
                for (OnGlobalSplitInstallCompleteListener<Integer> onCompleteListener : onCompleteListeners) {
                    if (onCompleteListener != null) {
                        onCompleteListener.onComplete(InstallTaskImpl.this);
                    }
                }

                return null;
            }
        });
    }

    private void notifyFailure(Exception exception, int errorCode) {
        this.exception = GlobalSplitInstallExceptionFactory.create(errorCode, exception);
        logger.e("Install failed", exception);
        updateSessionState(GlobalSplitInstallSessionStatus.FAILED, errorCode);
        notifyCompleted();
        executor.executeForeground(new Executor.Callbacks<Void>() {
            @Override
            Void execute() {
                for (OnGlobalSplitInstallFailureListener onFailureListener : onFailureListeners) {
                    if (onFailureListener != null) {
                        onFailureListener.onFailure(InstallTaskImpl.this.exception);
                    }
                }

                return null;
            }
        });
    }

    private void notifySuccess() {
        notifyCompleted();
        executor.executeForeground(new Executor.Callbacks<Void>() {
            @Override
            Void execute() {
                for (OnGlobalSplitInstallSuccessListener<? super Integer> onSuccessListener : onSuccessListeners) {
                    if (onSuccessListener != null) {
                        onSuccessListener.onSuccess(getResult());
                    }
                }

                return null;
            }
        });
    }

    @Override
    ApkInstaller getInstaller() {
        return apkInstaller;
    }

    @Override
    ApkDownloadRequest getDownloadRequest() {
        return apkDownloadRequest;
    }

    @Override
    public GlobalSplitInstallRequestInternal getInstallRequest() {
        return splitInstallRequest;
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        InstallService.InstallBinder installBinder = (InstallService.InstallBinder) iBinder;
        installBinder.getService().attach(this);
        isBoundToService = true;
        logger.i("Service connected");
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        isBoundToService = false;
        updateSessionState(GlobalSplitInstallSessionStatus.FAILED, GlobalSplitInstallErrorCode.SERVICE_DIED);
        logger.i("Service disconnected");
    }

    @Override
    void registerStateListener(GlobalSplitInstallUpdatedListener listener) {
        localInstallListeners.add(listener);
    }

    @Override
    public GlobalSplitInstallSessionState getCurrentState() {
        return currentState;
    }

    @Override
    public void cancel(int sessionId) {
        if (currentState != null
                && currentState.status() == GlobalSplitInstallSessionStatus.DOWNLOADING
                && apkDownloadRequest != null) {
            apkDownloadRequest.cancel();
        }
    }

    @Override
    public boolean isComplete() {
        return currentState != null && currentState.sessionId() > 0 || exception != null;
    }

    @Override
    public boolean isSuccessful() {
        return currentState != null && currentState.sessionId() > 0 && exception == null;
    }

    @Override
    public Exception getException() {
        return exception;
    }

    @Override
    public Integer getResult() {
        if (exception != null) {
            throw new RuntimeExecutionException(exception);
        }

        if (currentState != null && currentState.sessionId() > 0) {
            return currentState.sessionId();
        }

        throw new IllegalStateException("Task not yet completed"
                + (currentState != null ? ", status: " + currentState.status() : ""));
    }

    @Override
    public GlobalSplitInstallTask<Integer> addOnFailureListener(OnGlobalSplitInstallFailureListener listener) {
        if (isComplete() && !isSuccessful()) {
            listener.onFailure(exception);
        } else {
            onFailureListeners.add(listener);
        }

        return this;
    }

    @Override
    public GlobalSplitInstallTask<Integer> addOnFailureListener(
            final java.util.concurrent.Executor executor,
            final OnGlobalSplitInstallFailureListener listener
    ) {
        if (isComplete() && !isSuccessful()) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    listener.onFailure(exception);
                }
            });
        } else {
            onFailureListeners.add(new OnGlobalSplitInstallFailureListener() {
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
    public GlobalSplitInstallTask<Integer> addOnSuccessListener(OnGlobalSplitInstallSuccessListener<Integer> listener) {
        if (isSuccessful()) {
            listener.onSuccess(getResult());
        } else {
            onSuccessListeners.add(listener);
        }

        return this;
    }

    @Override
    public GlobalSplitInstallTask<Integer> addOnSuccessListener(
            final java.util.concurrent.Executor executor,
            final OnGlobalSplitInstallSuccessListener<Integer> listener) {
        if (isSuccessful()) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    listener.onSuccess(getResult());
                }
            });
        } else {
            onSuccessListeners.add(new OnGlobalSplitInstallSuccessListener<Integer>() {
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
    public GlobalSplitInstallTask<Integer> addOnCompleteListener(OnGlobalSplitInstallCompleteListener<Integer> listener) {
        if (isComplete()) {
            listener.onComplete(this);
        } else {
            onCompleteListeners.add(listener);
        }

        return this;
    }

    @Override
    public GlobalSplitInstallTask<Integer> addOnCompleteListener(
            final java.util.concurrent.Executor executor,
            final OnGlobalSplitInstallCompleteListener<Integer> listener) {
        if (isComplete()) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    listener.onComplete(InstallTaskImpl.this);
                }
            });
        } else {
            onCompleteListeners.add(new OnGlobalSplitInstallCompleteListener<Integer>() {
                @Override
                public void onComplete(final GlobalSplitInstallTask<Integer> task) {
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

