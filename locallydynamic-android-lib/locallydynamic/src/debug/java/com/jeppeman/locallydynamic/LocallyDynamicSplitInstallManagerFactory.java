package com.jeppeman.locallydynamic;

import android.content.Context;

import com.google.android.play.core.splitinstall.SplitInstallManager;
import com.jeppeman.locallydynamic.generated.LocallyDynamicBuildConfig;

public class LocallyDynamicSplitInstallManagerFactory {
    public static SplitInstallManager create(Context context) {
        return new LocallyDynamicSplitInstallManagerImpl(
                context,
                LocallyDynamicBuildConfigFactory.create(),
                LoggerFactory.create()
        );
    }
}