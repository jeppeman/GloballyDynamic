package com.jeppeman.locallydynamic;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInstaller;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.os.ResultReceiver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;

import com.jeppeman.locallydynamic.net.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class ApkInstallerFactory {
    static ApkInstaller create(
            @NonNull Context context,
            @NonNull Logger logger,
            @NonNull ApkInstaller.StatusListener statusListener) {
        ApkInstaller apkInstaller = new ApkInstallerImpl(context, statusListener, logger);
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

interface ApkInstaller {
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
            final int code;
            final Exception exception;

            Failed(String installMessage, int code) {
                this(installMessage, code, null);
            }

            Failed(String installMessage, int code, Exception exception) {
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
    /**
     * Value of {@link android.content.pm.PackageManager.INSTALL_ALLOW_TEST}
     */
    private static final int INSTALL_ALLOW_TEST = 0x00000004;
    /**
     * Value of {@link android.content.pm.PackageManager.INSTALL_DONT_KILL_APP}
     */
    private static final int INSTALL_DONT_KILL_APP = 0x00001000;

    private final Context context;
    private final StatusListener statusListener;
    private final Logger logger;
    @VisibleForTesting
    int sessionId = -1;

    ApkInstallerImpl(
            @NonNull Context context,
            @NonNull StatusListener statusListener,
            @NonNull Logger logger) {
        super(new Handler(Looper.getMainLooper()));
        this.context = context;
        this.statusListener = statusListener;
        this.logger = logger;
    }

    private void updateStatus(Status status) {
        if (status instanceof Status.Failed
                || status instanceof Status.Installed) {
            ApkInstallerRegistry.unregisterInstaller(this);
        }

        statusListener.onUpdate(status);
    }

    private void addInstallFlag(PackageInstaller.SessionParams sessionParams, int flag) {
        try {
            Field installFlagsField = sessionParams.getClass().getDeclaredField("installFlags");
            boolean wasAccessible = installFlagsField.isAccessible();
            installFlagsField.setAccessible(true);
            int installFlags = installFlagsField.getInt(sessionParams);
            installFlagsField.set(sessionParams, installFlags | flag);
            installFlagsField.setAccessible(wasAccessible);
        } catch (Exception exception) {
            updateStatus(new Status.Failed(
                    "Failed to install", -1, exception));
        }
    }

    private void setAllowTest(PackageInstaller.SessionParams sessionParams) {
        addInstallFlag(sessionParams, INSTALL_ALLOW_TEST);
    }

    private void setDontKillApp(PackageInstaller.SessionParams sessionParams) {
        addInstallFlag(sessionParams, INSTALL_DONT_KILL_APP);
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

    @Override
    public int getSessionId() {
        return sessionId;
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        Intent intent = resultData.getParcelable(Intent.EXTRA_INTENT);
        if (intent != null) {
            receiveInstallationResult(intent);
        } else {
            updateStatus(
                    new ApkInstaller.Status.Failed(
                            Intent.EXTRA_INTENT + " not found " +
                                    "in intent, this should never happen",
                            -1
                    )
            );
        }
    }

    @Override
    public void receiveInstallationResult(@NonNull Intent intent) {
        logger.i("Received install result to " + this + ", intent: " + intent);
        int sessionId = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1);
        if (sessionId == this.sessionId) {
            int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, 0);
            String message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
            Status installStatus;
            if (status == PackageInstaller.STATUS_SUCCESS) {
                installStatus = new ApkInstaller.Status.Installed(message);
            } else if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
                Intent confirmIntent = intent.getParcelableExtra(Intent.EXTRA_INTENT);
                if (confirmIntent != null) {
                    confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(confirmIntent);
                }
                installStatus = new ApkInstaller.Status.Pending(message);
            } else {
                installStatus = new ApkInstaller.Status.Failed(message, status);
            }

            updateStatus(installStatus);
        } else {
            updateStatus(
                    new ApkInstaller.Status.Failed(
                            PackageInstaller.EXTRA_SESSION_ID + " not found " +
                                    "in intent, this should never happen",
                            -1
                    )
            );
        }
    }

    @Override
    public void install(List<File> apks) {
        if (apks.isEmpty()) {
            updateStatus(new Status.Failed("apks must not be empty", -1));
            return;
        }

        try {
            long totalSize = 0;
            for (File apk : apks) {
                totalSize += apk.length();
            }

            PackageInstaller.SessionParams sessionParams = new PackageInstaller.SessionParams(
                    PackageInstaller.SessionParams.MODE_INHERIT_EXISTING
            );

            setAllowTest(sessionParams);
            setDontKillApp(sessionParams);
            sessionParams.setAppPackageName(context.getPackageName());
            sessionParams.setSize(totalSize);

            PackageInstaller.Session session = createSession(sessionParams);

            updateStatus(new ApkInstaller.Status.Installing());

            for (File file : apks) {
                long fileSize = file.length();
                OutputStream outStream = session.openWrite(file.getName(), 0, fileSize);
                byte[] bytes = FileUtils.readAllBytes(file);
                outStream.write(bytes);
                session.fsync(outStream);
                outStream.close();
            }

            Intent nestedIntent = new Intent().putExtra(
                    PackageInstallerResultReceiver.EXTRA_RESULT_RECEIVER,
                    this
            );
            Intent intent = new Intent(context, PackageInstallerResultReceiver.class);
            intent.putExtra(
                    PackageInstallerResultReceiver.EXTRA_NESTED_INTENT,
                    nestedIntent
            );

            session.commit(getIntentSenderForSession(intent));
            session.close();
        } catch (Exception exception) {
            updateStatus(new Status.Failed(
                    "Failed to install", -1, exception));
        }
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
}