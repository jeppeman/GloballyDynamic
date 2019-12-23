package com.jeppeman.globallydynamic.globalsplitcompat;

import android.content.Context;

import com.huawei.hms.feature.dynamicinstall.FeatureCompat;

/**
 * Enables immediate access to code and resources of split APK:s installed
 * through {@link com.jeppeman.globallydynamic.globalsplitinstall.GlobalSplitInstallManager}.
 *
 * This class delegates to {@link FeatureCompat}
 */
public class GlobalSplitCompat {
    /**
     * @see FeatureCompat#install(Context)
     */
    public static boolean install(Context context) {
        return FeatureCompat.install(context);
    }

    /**
     * @see FeatureCompat#install(Context)
     */
    public static boolean installActivity(Context context) {
        return FeatureCompat.install(context);
    }

    /**
     * @see FeatureCompat#install(Context)
     */
    public static boolean a(Context context) {
        return FeatureCompat.install(context);
    }

    /**
     * No-op for Huawei
     *
     * @return always false
     */
    public static boolean a() {
        // No-op for Huawei
        return false;
    }
}
