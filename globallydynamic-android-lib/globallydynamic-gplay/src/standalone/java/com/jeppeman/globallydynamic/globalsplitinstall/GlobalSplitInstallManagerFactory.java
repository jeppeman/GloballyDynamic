package com.jeppeman.globallydynamic.globalsplitinstall;

import android.content.Context;

import com.google.android.play.core.splitinstall.SplitInstallManagerFactory;

/**
 * Creates {@link GlobalSplitInstallManager}s
 */
public class GlobalSplitInstallManagerFactory {
    /**
     * Creates a new {@link GlobalSplitInstallManager} that delegates to an underlying
     * {@link com.google.android.play.core.splitinstall.SplitInstallManager}
     *
     * @param context the application context
     * @return a new {@link GlobalSplitInstallManager}
     *
     * @see com.google.android.play.core.splitinstall.SplitInstallManager
     */
    public static GlobalSplitInstallManager create(Context context) {
        return new GPlayGlobalSplitInstallManager(SplitInstallManagerFactory.create(context));
    }
}
