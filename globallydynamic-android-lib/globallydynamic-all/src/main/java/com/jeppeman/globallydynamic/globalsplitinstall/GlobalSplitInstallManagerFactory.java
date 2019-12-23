package com.jeppeman.globallydynamic.globalsplitinstall;

import android.content.Context;

import com.google.android.play.core.splitinstall.SplitInstallManagerFactory;
import com.huawei.hms.feature.install.FeatureInstallManagerFactory;

/**
 * Creates {@link GlobalSplitInstallManager}s based on {@link GlobalSplitInstallProvider}s
 */
public class GlobalSplitInstallManagerFactory {
    /**
     * Creates a {@link GlobalSplitInstallManager} based on given provider.
     *
     * @param context your application context
     * @param provider the underlying provider of dynamic delivery
     * @return a {@link GlobalSplitInstallManager} based on the provider given
     *
     * @see GlobalSplitInstallProvider
     */
    public static GlobalSplitInstallManager create(Context context, @GlobalSplitInstallProvider int provider) {
        switch (provider) {
            case GlobalSplitInstallProvider.SELF_HOSTED:
                return new GlobalSplitInstallManagerImpl(
                        context,
                        GloballyDynamicBuildConfigFactory.create()
                );
            case GlobalSplitInstallProvider.GOOGLE_PLAY:
                return new GPlayGlobalSplitInstallManager(SplitInstallManagerFactory.create(context));
            case GlobalSplitInstallProvider.HUAWEI:
                return new HuaweiGlobalSplitInstallManager(FeatureInstallManagerFactory.create(context));
            default:
                throw new IllegalArgumentException("Unrecognized provider " + provider);
        }
    }
}