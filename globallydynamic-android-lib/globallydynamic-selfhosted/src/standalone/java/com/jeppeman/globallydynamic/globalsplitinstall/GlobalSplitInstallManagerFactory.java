package com.jeppeman.globallydynamic.globalsplitinstall;

import android.content.Context;

/**
 * Creates {@link GlobalSplitInstallManager}s
 */
public class GlobalSplitInstallManagerFactory {
    /**
     * Creates a new {@link GlobalSplitInstallManager} that downloads splits from
     * a GloballyDynamic server and installs them
     *
     * @param context the application context
     * @return a new {@link GlobalSplitInstallManager}
     */
    public static GlobalSplitInstallManager create(Context context) {
        return new GlobalSplitInstallManagerImpl(
                context,
                GloballyDynamicBuildConfigFactory.create()
        );
    }
}
