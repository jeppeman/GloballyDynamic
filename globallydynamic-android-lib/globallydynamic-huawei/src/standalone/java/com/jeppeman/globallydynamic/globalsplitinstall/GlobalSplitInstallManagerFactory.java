package com.jeppeman.globallydynamic.globalsplitinstall;

import android.content.Context;

import com.huawei.hms.feature.install.FeatureInstallManagerFactory;

/**
 * Creates {@link GlobalSplitInstallManager}s
 */
public class GlobalSplitInstallManagerFactory {
    /**
     * Creates a new {@link GlobalSplitInstallManager} that delegates to an underlying
     * {@link com.huawei.hms.feature.install.FeatureInstallManager}
     *
     * @param context the application context
     * @return a new {@link GlobalSplitInstallManager}
     *
     * @see com.huawei.hms.feature.install.FeatureInstallManager
     */
    public static GlobalSplitInstallManager create(Context context) {
        return new HuaweiGlobalSplitInstallManager(FeatureInstallManagerFactory.create(context));
    }
}
