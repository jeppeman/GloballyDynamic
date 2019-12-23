package com.jeppeman.globallydynamic.globalsplitinstall;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.ResultReceiver;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY)
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class InstallCancelReceiver extends BroadcastReceiver {
    static final String EXTRA_RECEIVER = InstallCancelReceiver.class.getCanonicalName() + ".RECEIVER";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (InstallService.ACTION_CANCELED.equals(intent.getAction())) {
            ResultReceiver receiver = intent.getParcelableExtra(EXTRA_RECEIVER);
            if (receiver != null) {
                Bundle arguments = new Bundle();
                arguments.putBoolean(InstallService.ACTION_CANCELED, true);
                receiver.send(0, arguments);
            }
        }
    }
}
