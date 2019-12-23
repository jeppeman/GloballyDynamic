package com.jeppeman.globallydynamic.globalsplitinstall;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInstaller;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.ResultReceiver;

import com.jeppeman.globallydynamic.selfhosted.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class ApkInstallerFactory {
    static ApkInstaller create(
            @NonNull Context context,
            @NonNull Logger logger,
            @NonNull Executor executor,
            @NonNull ApplicationPatcher applicationPatcher,
            @NonNull ApkInstaller.StatusListener statusListener) {
        ApkInstaller apkInstaller = new ApkInstallerImpl(
                context,
                applicationPatcher,
                statusListener,
                logger,
                executor
        );
        ApkInstallerRegistry.registerInstaller(apkInstaller);
        return apkInstaller;
    }
}

class ApkInstallerRegistry {
    private static List<ApkInstaller> apkInstallers = new LinkedList<ApkInstaller>();

    static void registerInstaller(@NonNull ApkInstaller apkInstaller) {
        apkInstallers.add(apkInstaller);
    }

    static void unregisterInstaller(@NonNull ApkInstaller apkInstaller) {
        apkInstallers.remove(apkInstaller);
    }

    @Nullable
    static ApkInstaller findInstaller(int sessionId) {
        for (ApkInstaller apkInstaller : apkInstallers) {
            if (apkInstaller.getSessionId() == sessionId) {
                return apkInstaller;
            }
        }
        return null;
    }
}

interface ApkInstaller extends Parcelable {
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    void install(List<File> apks);

    int getSessionId();

    void receiveInstallationResult(@NonNull Intent intent);

    abstract class Status {
        final String title;
        final String installMessage;

        Status(String title, String installMessage) {
            this.title = title;
            this.installMessage = installMessage;
        }

        static class Installing extends Status {
            Installing() {
                super("Installing", "");
            }
        }

        static class Installed extends Status {
            Installed(String installMessage) {
                super("Success", installMessage);
            }
        }

        static class Failed extends Status {
            @GlobalSplitInstallErrorCode
            final int code;
            final Exception exception;

            Failed(String installMessage, @GlobalSplitInstallErrorCode int code) {
                this(installMessage, code, null);
            }

            Failed(String installMessage, @GlobalSplitInstallErrorCode int code, Exception exception) {
                super("Failure", installMessage);
                this.code = code;
                this.exception = exception != null
                        ? exception
                        : new Exception(installMessage);
            }
        }

        static class Pending extends Status {
            Pending(String installMessage) {
                super("Pending", installMessage);
            }
        }

        static class RequiresUserPermission extends Status {
            RequiresUserPermission() {
                super("RequiresUserPermission", "");
            }
        }

        static class Canceled extends Status {
            Canceled() {
                super("Canceled", "");
            }
        }

        @NonNull
        @Override
        public String toString() {
            return String.format(
                    Locale.ENGLISH,
                    "%s(title=%s, installMessage=%s)",
                    getClass().getSimpleName(),
                    title,
                    installMessage
            );
        }
    }

    interface StatusListener {
        void onUpdate(Status status);
    }
}

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class ApkInstallerImpl extends ResultReceiver implements ApkInstaller {
    private final Context context;
    private final ApplicationPatcher applicationPatcher;
    private final StatusListener statusListener;
    private final Logger logger;
    private final Executor executor;
    private List<File> apks;
    @VisibleForTesting
    int sessionId = -1;

    ApkInstallerImpl(
            @NonNull Context context,
            @NonNull ApplicationPatcher applicationPatcher,
            @NonNull StatusListener statusListener,
            @NonNull Logger logger,
            @NonNull Executor executor) {
        super(executor.getForegroundHandler());
        this.context = context;
        this.applicationPatcher = applicationPatcher;
        this.statusListener = statusListener;
        this.logger = logger;
        this.executor = executor;
    }

    private void updateStatus(Status status) {
        if (status instanceof Status.Failed
                || status instanceof Status.Canceled
                || status instanceof Status.Installed) {
            ApkInstallerRegistry.unregisterInstaller(this);
        }

        if (status instanceof Status.Canceled
                || status instanceof Status.Failed) {
            if (apks != null) {
                for (File apk : apks) {
                    apk.delete();
                }
            }
        }

        statusListener.onUpdate(status);
    }

    private void setDontKillApp(PackageInstaller.SessionParams sessionParams) {
        try {
            ReflectionUtils.invokeMethod(sessionParams, "setDontKillApp", true);
        } catch (Exception exception) {
            updateStatus(new Status.Failed(
                    "Failed to install", GlobalSplitInstallErrorCode.INTERNAL_ERROR, exception));
        }
    }

    private boolean isMarshmallowOrBelow() {
        return Build.VERSION.SDK_INT <= Build.VERSION_CODES.M;
    }

    private boolean requiresRestart() {
        return isMarshmallowOrBelow();
    }

    private String restartMessage() {
        return context.getString(R.string.restart_message_marshmallow);
    }

    @VisibleForTesting
    PackageInstaller.Session createSession(@NonNull PackageInstaller.SessionParams sessionParams) throws IOException {
        PackageInstaller packageInstaller = context.getPackageManager().getPackageInstaller();
        sessionId = packageInstaller.createSession(sessionParams);
        return context.getPackageManager().getPackageInstaller().openSession(sessionId);
    }

    @VisibleForTesting
    IntentSender getIntentSenderForSession(@NonNull Intent intent) {
        return PendingIntent.getBroadcast(
                context,
                sessionId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
        ).getIntentSender();
    }

    @VisibleForTesting
    void doInstall(final List<File> apks) throws Exception {
        PackageInstaller.SessionParams sessionParams = new PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_INHERIT_EXISTING
        );

        long totalSize = 0;
        for (File apk : apks) {
            totalSize += apk.length();
        }

        setDontKillApp(sessionParams);
        sessionParams.setAppPackageName(context.getPackageName());
        sessionParams.setSize(totalSize);

        PackageInstaller.Session session = createSession(sessionParams);

        byte[] buffer = new byte[65536];
        for (File file : apks) {
            long fileSize = file.length();
            OutputStream outStream = session.openWrite(file.getName(), 0, fileSize);
            FileInputStream fileInputStream = new FileInputStream(file);
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                outStream.write(buffer, 0, bytesRead);
            }
            session.fsync(outStream);
            outStream.close();
        }

        Intent nestedIntent = new Intent().putExtra(
                PackageInstallerResultReceiver.EXTRA_RESULT_RECEIVER,
                ApkInstallerImpl.this
        ).putExtra(
                PackageInstallerResultReceiver.EXTRA_NEEDS_RESTART,
                requiresRestart()
        ).putExtra(
                PackageInstallerResultReceiver.EXTRA_RESTART_MESSAGE,
                restartMessage()
        );
        Intent intent = new Intent(context, PackageInstallerResultReceiver.class);
        intent.putExtra(
                PackageInstallerResultReceiver.EXTRA_NESTED_INTENT,
                nestedIntent
        );

        session.commit(getIntentSenderForSession(intent));
        session.close();
    }

    @VisibleForTesting
    void onInstallError(Exception exception) {
        updateStatus(new Status.Failed(
                "Failed to install", GlobalSplitInstallErrorCode.INTERNAL_ERROR, exception));
    }

    private void cancel() {
        try {
            context.getPackageManager()
                    .getPackageInstaller()
                    .abandonSession(sessionId);
        } catch (Exception exception) {
            logger.i("Failed to abandon session, was probably already abandoned by the system");
        }
    }

    @Override
    public int getSessionId() {
        return sessionId;
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        Intent intent = resultData.getParcelable(Intent.EXTRA_INTENT);
        boolean canceled = resultData.getBoolean(InstallService.ACTION_CANCELED, false);
        if (intent != null) {
            receiveInstallationResult(intent);
        } else if (canceled) {
            cancel();
        }  else {
            updateStatus(
                    new ApkInstaller.Status.Failed(
                            Intent.EXTRA_INTENT + " not found " +
                                    "in intent, this should never happen",
                            GlobalSplitInstallErrorCode.INTERNAL_ERROR
                    )
            );
        }
    }

    @Override
    public void receiveInstallationResult(@NonNull Intent intent) {
        logger.i("Received install result to " + this + ", intent: " + StringUtils.fromIntent(intent));
        int sessionId = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1);
        PackageInstaller.SessionInfo sessionInfo = context.getPackageManager()
                .getPackageInstaller()
                .getSessionInfo(sessionId);
        if (sessionId == this.sessionId) {
            int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, 0);
            final String message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
            if (status == PackageInstaller.STATUS_SUCCESS) {
                executor.execute(new Executor.Callbacks<Void>() {
                    @Override
                    Void execute() {
                        applicationPatcher.patchExistingApplication();
                        return null;
                    }

                    @Override
                    void onComplete(Void result) {
                        updateStatus(new ApkInstaller.Status.Installed(message));
                    }

                    @Override
                    void onError(Exception exception) {
                        onInstallError(exception);
                    }
                });
            } else if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
                Intent confirmIntent = intent.getParcelableExtra(Intent.EXTRA_INTENT);
                if (confirmIntent != null) {
                    context.startActivity(confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                }
            } else if (status == PackageInstaller.STATUS_FAILURE_ABORTED) {
                updateStatus(new Status.Canceled());
            } else {
                updateStatus(new ApkInstaller.Status.Failed(
                        "Error code " + status + ": " + message,
                        GlobalSplitInstallErrorCode.INTERNAL_ERROR
                ));
            }
        } else {
            updateStatus(
                    new ApkInstaller.Status.Failed(
                            PackageInstaller.EXTRA_SESSION_ID + " not found " +
                                    "in intent, this should never happen",
                            GlobalSplitInstallErrorCode.INTERNAL_ERROR
                    )
            );
        }
    }

    @Override
    public void install(final List<File> apks) {
        if (apks.isEmpty()) {
            updateStatus(new Status.Failed("apks must not be empty", GlobalSplitInstallErrorCode.INTERNAL_ERROR));
            return;
        }

        this.apks = apks;
        updateStatus(new ApkInstaller.Status.Installing());
        executor.execute(new ExecutorInstallCallbacks(apks));
    }

    @NonNull
    @Override
    public String toString() {
        return String.format(
                Locale.ENGLISH,
                "%s(sessionId=%d)",
                getClass().getSimpleName(),
                sessionId
        );
    }

    private class ExecutorInstallCallbacks extends Executor.Callbacks<Void> {

        private final List<File> apks;

        ExecutorInstallCallbacks(List<File> apks) {
            this.apks = apks;
        }

        @Override
        Void execute() throws Exception {
            doInstall(apks);
            return null;
        }

        @Override
        void onError(Exception exception) {
            onInstallError(exception);
        }
    }
}
