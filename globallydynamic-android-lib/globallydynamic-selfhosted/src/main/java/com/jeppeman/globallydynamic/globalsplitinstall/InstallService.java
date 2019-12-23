package com.jeppeman.globallydynamic.globalsplitinstall;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import com.jeppeman.globallydynamic.net.ListUtils;
import com.jeppeman.globallydynamic.selfhosted.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.app.NotificationCompat;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class InstallService extends Service {
    private static final int NOTIFICATION_ID = 938436372;
    private static final String NOTIFICATION_CHANNEL_ID = "GloballyDynamic";
    static final String ACTION_CANCELED = InstallService.class.getCanonicalName() + ".ACTION_CANCELED";

    private IBinder binder = new InstallBinder();
    private NotificationManager notificationManager;
    private final Map<Integer, ActiveInstallation> activeInstallations =
            Collections.synchronizedMap(new LinkedHashMap<Integer, ActiveInstallation>());

    private void removeInstallation(int id) {
        activeInstallations.remove(id);
        notificationManager.cancelAll();

        Collection<ActiveInstallation> currentInstallations = new ArrayList<ActiveInstallation>(activeInstallations.values());
        activeInstallations.clear();
        int i = 0;
        for (ActiveInstallation active : currentInstallations) {
            activeInstallations.put(NOTIFICATION_ID + i, active);
        }

        for (Map.Entry<Integer, ActiveInstallation> entry : activeInstallations.entrySet()) {
            notificationManager.notify(entry.getKey(), entry.getValue().buildNotification());
        }
    }

    private int addInstallation(ActiveInstallation activeInstallation) {
        final int notificationId = NOTIFICATION_ID + activeInstallations.size();
        activeInstallations.put(notificationId, activeInstallation);
        return notificationId;
    }

    private Map.Entry<Integer, ActiveInstallation> findInstallationByState(GlobalSplitInstallSessionState state) {
        for (Map.Entry<Integer, ActiveInstallation> entry : activeInstallations.entrySet()) {
            if (entry.getValue().installSessionState.sessionId() == state.sessionId()) {
                return entry;
            }
        }

        return null;
    }

    private void kill() {
        stopForeground(true);
        stopSelf();
    }

    synchronized void attach(final InstallTask task) {
        task.registerStateListener(new GlobalSplitInstallUpdatedListener() {
            @Override
            public void onStateUpdate(GlobalSplitInstallSessionState state) {
                synchronized (activeInstallations) {
                    Map.Entry<Integer, ActiveInstallation> maybeInstallation = findInstallationByState(state);
                    ActiveInstallation activeInstallation;
                    int notificationId;
                    if (maybeInstallation == null) {
                        activeInstallation = new ActiveInstallation(
                                InstallService.this,
                                createNotificationBuilder(),
                                state,
                                task
                        );
                        notificationId = addInstallation(activeInstallation);
                    } else {
                        activeInstallation = maybeInstallation.getValue();
                        notificationId = maybeInstallation.getKey();
                    }
                    activeInstallation.installSessionState = state;
                    notificationManager.notify(notificationId, activeInstallation.buildNotification());
                    if (state.status() == GlobalSplitInstallSessionStatus.CANCELED
                            || state.status() == GlobalSplitInstallSessionStatus.INSTALLED
                            || state.status() == GlobalSplitInstallSessionStatus.FAILED) {

                        removeInstallation(notificationId);

                        if (activeInstallations.isEmpty()) {
                            kill();
                        }
                    }
                }
            }
        });
        task.start();
    }

    private NotificationCompat.Builder createNotificationBuilder() {
        NotificationCompat.Builder notificationBuilder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    NOTIFICATION_CHANNEL_ID,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            notificationManager.createNotificationChannel(notificationChannel);
            notificationBuilder = new NotificationCompat.Builder(this, notificationChannel.getId());
        } else {
            notificationBuilder = new NotificationCompat.Builder(this);
        }

        notificationBuilder.setSmallIcon(android.R.drawable.stat_sys_download);
        notificationBuilder.setOngoing(true);

        return notificationBuilder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = createNotificationBuilder().build();
        startForeground(NOTIFICATION_ID, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    class InstallBinder extends Binder {
        InstallService getService() {
            return InstallService.this;
        }
    }

    static class ActiveInstallation {
        private final Context context;
        private final InstallTask task;
        private GlobalSplitInstallSessionState installSessionState;
        private String title;
        private NotificationCompat.Builder notificationBuilder;

        ActiveInstallation(
                @NonNull Context context,
                @NonNull NotificationCompat.Builder notificationBuilder,
                @NonNull GlobalSplitInstallSessionState installSessionState,
                @NonNull InstallTask task
        ) {
            this.context = context;
            this.notificationBuilder = notificationBuilder;
            this.installSessionState = installSessionState;
            this.task = task;
        }

        @SuppressLint("RestrictedApi")
        private void addCancelAction() {
            notificationBuilder.mActions.clear();
            Intent cancelIntent = new Intent(context, InstallCancelReceiver.class);
            cancelIntent.setAction(InstallService.ACTION_CANCELED);
            switch (installSessionState.status()) {
                case GlobalSplitInstallSessionStatus.DOWNLOADING:
                    cancelIntent.putExtra(InstallCancelReceiver.EXTRA_RECEIVER, task.getDownloadRequest());
                    break;
                case GlobalSplitInstallSessionStatus.INSTALLING:
                    cancelIntent.putExtra(InstallCancelReceiver.EXTRA_RECEIVER, task.getInstaller());
                    break;
                default:
                    cancelIntent = null;
            }

            if (cancelIntent != null) {
                PendingIntent pendingCancelIntent = PendingIntent.getBroadcast(
                        context,
                        installSessionState.sessionId(),
                        cancelIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
                notificationBuilder.addAction(
                        R.drawable.ic_baseline_close_24,
                        context.getString(R.string.cancel),
                        pendingCancelIntent
                );
            }
        }

        private Notification buildNotification() {
            if (title == null) {
                List<String> titleItems = new LinkedList<String>(installSessionState.moduleNames());
                titleItems.addAll(installSessionState.languages());
                title = ListUtils.toString(titleItems, false, true);
            }

            addCancelAction();

            int progress = installSessionState.totalBytesToDownload() > 0
                    ? Math.round((installSessionState.bytesDownloaded()
                    / (float) installSessionState.totalBytesToDownload()) * 100)
                    : 0;

            String progressText = progress + "%";

            notificationBuilder.setContentTitle(title);
            notificationBuilder.setContentText(progress >= 100 ? "" : progressText);

            switch (installSessionState.status()) {
                case GlobalSplitInstallSessionStatus.PENDING:
                    notificationBuilder.setContentText(context.getString(R.string.status_pending));
                    notificationBuilder.setProgress(100, progress, true);
                    break;
                case GlobalSplitInstallSessionStatus.DOWNLOADING:
                case GlobalSplitInstallSessionStatus.DOWNLOADED:
                    notificationBuilder.setContentText(progressText);
                    notificationBuilder.setProgress(100, progress, false);
                    break;
                case GlobalSplitInstallSessionStatus.INSTALLING:
                    notificationBuilder.setContentText(context.getString(R.string.status_installing));
                    notificationBuilder.setProgress(100, progress, true);
                    break;
                case GlobalSplitInstallSessionStatus.INSTALLED:
                    notificationBuilder.setContentText(context.getString(R.string.status_installed));
                    notificationBuilder.setProgress(100, progress, false);
                    break;
                case GlobalSplitInstallSessionStatus.CANCELING:
                    notificationBuilder.setContentText(context.getString(R.string.status_canceling));
                    notificationBuilder.setProgress(100, progress, false);
                    break;
                case GlobalSplitInstallSessionStatus.CANCELED:
                    notificationBuilder.setContentText(context.getString(R.string.status_canceled));
                    notificationBuilder.setProgress(100, progress, false);
                    break;
                case GlobalSplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION:
                    notificationBuilder.setContentText(context.getString(R.string.status_requires_confirmation));
                    notificationBuilder.setProgress(100, progress, true);
                    break;
                case GlobalSplitInstallSessionStatus.FAILED:
                    notificationBuilder.setContentText(context.getString(R.string.status_failed));
                    notificationBuilder.setProgress(100, progress, true);
                    break;
                case GlobalSplitInstallSessionStatus.REQUIRES_PERSON_AGREEMENT:
                case GlobalSplitInstallSessionStatus.UNKNOWN:
                    break;
            }

            addCancelAction();

            return notificationBuilder.build();
        }
    }

    static Intent getIntent(@NonNull Context context) {
        return new Intent(context, InstallService.class);
    }

    static void start(@NonNull Context context) {
        Intent intent = getIntent(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }
}
