package com.jeppeman.globallydynamic.globalsplitinstall;

import androidx.annotation.IntDef;

@IntDef({
        GlobalSplitInstallProvider.SELF_HOSTED,
        GlobalSplitInstallProvider.GOOGLE_PLAY,
        GlobalSplitInstallProvider.HUAWEI
})
public @interface GlobalSplitInstallProvider {
    /**
     * Self hosted using GloballyDynamic Server
     */
    int SELF_HOSTED = 1;
    /**
     * Google Play Store
     */
    int GOOGLE_PLAY = 2;
    /**
     * Huawei App Gallery
     */
    int HUAWEI = 3;
}
