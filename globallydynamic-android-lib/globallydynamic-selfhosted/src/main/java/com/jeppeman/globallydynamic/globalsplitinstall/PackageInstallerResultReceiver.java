package com.jeppeman.globallydynamic.globalsplitinstall;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.Build;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.widget.Toast;

import com.jeppeman.globallydynamic.generated.GloballyDynamicBuildConfig;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY)
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class PackageInstallerResultReceiver extends BroadcastReceiver {
    private static final String EXTRA_PREFIX =
            "com.jeppeman.globallydynamic.globalsplitinstall.PackageInstallerResultReceiver";
    static final String EXTRA_NESTED_INTENT = EXTRA_PREFIX + ".NESTED_INTENT";
    static final String EXTRA_RESULT_RECEIVER = EXTRA_PREFIX + ".RESULT_RECEIVER";
    static final String EXTRA_NEEDS_RESTART = EXTRA_PREFIX + ".NEEDS_RESTART";
    static final String EXTRA_RESTART_MESSAGE = EXTRA_PREFIX + ".RESTART_MESSAGE";
    private static final int BOGUS_SESSION_ID = -10;
    private final Logger logger = LoggerFactory.create();
    private final GloballyDynamicBuildConfig globallyDynamicBuildConfig =
            GloballyDynamicBuildConfigFactory.create();

    @Nullable
    private Intent getNestedIntent(Intent intent) {
        Intent nestedIntent = intent.getParcelableExtra(EXTRA_NESTED_INTENT);
        if (nestedIntent != null) {
            nestedIntent.setExtrasClassLoader(ApkInstallerImpl.class.getClassLoader());
        }

        return nestedIntent;
    }

    @Nullable
    private ResultReceiver extractReceiverFromIntent(Intent intent) {
        Intent nestedIntent = getNestedIntent(intent);
        if (nestedIntent != null) {
            return nestedIntent.getParcelableExtra(EXTRA_RESULT_RECEIVER);
        }

        return null;
    }

    private boolean extraRequiresRestart(Intent intent) {
        Intent nestedIntent = getNestedIntent(intent);
        if (nestedIntent != null) {
            return nestedIntent.getBooleanExtra(EXTRA_NEEDS_RESTART, false);
        }

        return false;
    }

    @Nullable
    private String extraRestartMessage(Intent intent) {
        Intent nestedIntent = getNestedIntent(intent);
        if (nestedIntent != null) {
            return nestedIntent.getStringExtra(EXTRA_RESTART_MESSAGE);
        }

        return null;
    }

    private void launchMainActivity(Context context, String message) {
        try {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                return;
            }

            Class<?> mainActivityClass = Class.forName(
                    globallyDynamicBuildConfig.getMainActivityFullyQualifiedName());
            Intent mainActivityIntent = new Intent(
                    context.getApplicationContext(),
                    mainActivityClass
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(mainActivityIntent);
        } catch (Exception exception) {
            logger.e("Failed to launch main activity", exception);
        }

        if (message != null) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            int sessionId = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, BOGUS_SESSION_ID);
            int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1);
            Intent extraIntent = intent.getParcelableExtra(Intent.EXTRA_INTENT);
            logger.i("Received broadcast to PackageInstallerResultReceiver, sessionId="
                    + sessionId + ", status=" + status + ", intent=" + StringUtils.fromIntent(intent)
                    + ", extraIntent=" + StringUtils.fromIntent(extraIntent)
                    + ", nestedIntent=" + StringUtils.fromIntent(getNestedIntent(intent)));
            if (sessionId != BOGUS_SESSION_ID) {
                ApkInstaller apkInstaller = ApkInstallerRegistry.findInstaller(sessionId);
                ResultReceiver resultReceiver = extractReceiverFromIntent(intent);
                if (apkInstaller != null) {
                    apkInstaller.receiveInstallationResult(intent);
                } else if (resultReceiver != null) {
                    boolean requiresRestart = extraRequiresRestart(intent);
                    if (status == PackageInstaller.STATUS_SUCCESS && requiresRestart) {
                        String message = extraRestartMessage(intent);
                        logger.i(message);
                        launchMainActivity(context, message);
                    } else {
                        Bundle bundle = new Bundle();
                        bundle.putParcelable(Intent.EXTRA_INTENT, intent);
                        resultReceiver.send(0, bundle);
                    }
                } else if (status == PackageInstaller.STATUS_SUCCESS) {
                    String restartMessage = extraRestartMessage(intent);
                    if (restartMessage != null) {
                        logger.i(restartMessage);
                    }
                    launchMainActivity(context, restartMessage);
                } else {
                    throw new IllegalStateException("ApkInstaller with id " + sessionId
                            + " not found, this should not happen");
                }
            } else {
                throw new IllegalStateException(PackageInstaller.EXTRA_SESSION_ID
                        + " not found int intent, this should not happen");
            }
        } else {
            throw new IllegalStateException("android.os.Build.VERSION.SDK_INT < 21, " +
                    "this should not happen");
        }
    }
}
