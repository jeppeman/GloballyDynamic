package com.jeppeman.locallydynamic;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.os.ResultReceiver;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.jeppeman.locallydynamic.generated.LocallyDynamicBuildConfig;

public class PackageInstallerResultReceiver extends BroadcastReceiver {
    private static final String EXTRA_PREFIX =
            "com.jeppeman.locallydynamic.PackageInstallerResultReceiver";
    static final String EXTRA_NESTED_INTENT = EXTRA_PREFIX + ".NESTED_INTENT";
    static final String EXTRA_RESULT_RECEIVER = EXTRA_PREFIX + ".RESULT_RECEIVER";
    private static final int BOGUS_SESSION_ID = -10;
    private final Logger logger = LoggerFactory.create();
    private final LocallyDynamicBuildConfig locallyDynamicBuildConfig =
            LocallyDynamicBuildConfigFactory.create();

    @Nullable
    private ResultReceiver extractReceiverFromIntent(Intent intent) {
        Intent nestedIntent = intent.getParcelableExtra(EXTRA_NESTED_INTENT);
        if (nestedIntent != null) {
            nestedIntent.setExtrasClassLoader(ApkInstallerImpl.class.getClassLoader());
            return nestedIntent.getParcelableExtra(EXTRA_RESULT_RECEIVER);
        }

        return null;
    }

    private void launchMainActivity(Context context, String message) {
        try {
            Class<?> mainActivityClass = Class.forName(
                    locallyDynamicBuildConfig.getMainActivityFullyQualifiedName());
            Intent mainActivityIntent = new Intent(
                    context.getApplicationContext(),
                    mainActivityClass
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(mainActivityIntent);
        } catch (Exception exception) {
            logger.e("Failed to launch main activity", exception);
        }

        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    private boolean isMarshmallowOrBelow() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.N;
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void onReceive(Context context, Intent intent) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            int sessionId = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, BOGUS_SESSION_ID);
            int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1);
            Intent extraIntent = intent.getParcelableExtra(Intent.EXTRA_INTENT);
            logger.i("Received broadcast to PackageInstallerResultReceiver, sessionId="
                    + sessionId + ", status=" + status + ", intent=" + extraIntent);
            if (sessionId != BOGUS_SESSION_ID) {
                ApkInstaller apkInstaller = ApkInstallerRegistry.findInstaller(sessionId);
                ResultReceiver resultReceiver = extractReceiverFromIntent(intent);
                if (apkInstaller != null) {
                    apkInstaller.receiveInstallationResult(intent);
                } else if (resultReceiver != null) {
                    if (status == PackageInstaller.STATUS_SUCCESS && isMarshmallowOrBelow()) {
                        String message = "Install successful, but install without restart not " +
                                "available for sdk version < 24, restarting...";
                        logger.i(message);
                        launchMainActivity(context, message);
                    } else {
                        Bundle bundle = new Bundle();
                        bundle.putParcelable(Intent.EXTRA_INTENT, intent);
                        resultReceiver.send(0, bundle);
                    }
                } else if (status == PackageInstaller.STATUS_SUCCESS) {
                    if (isMarshmallowOrBelow()) {
                        String message = "Install successful, but install without restart not " +
                                "available for sdk version < 24, restarting...";
                        logger.i(message);
                        launchMainActivity(context, message);
                    } else {
                        String message = "Install successful but live update did not work, this " +
                                "should not happen, restarting...";
                        logger.i(message);
                        launchMainActivity(context, message);
                    }
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
