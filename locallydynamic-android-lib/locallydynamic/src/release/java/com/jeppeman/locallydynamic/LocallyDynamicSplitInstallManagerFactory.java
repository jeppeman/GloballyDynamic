package com.jeppeman.locallydynamic;

import android.content.Context;

import com.google.android.play.core.splitinstall.SplitInstallManager;
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory;

public class LocallyDynamicSplitInstallManagerFactory {
    public static SplitInstallManager create(Context context) {
        return SplitInstallManagerFactory.create(context);
    }
}
