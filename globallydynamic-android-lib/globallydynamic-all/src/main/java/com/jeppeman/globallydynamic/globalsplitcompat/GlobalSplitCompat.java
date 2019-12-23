package com.jeppeman.globallydynamic.globalsplitcompat;

import android.content.Context;

import com.google.android.play.core.splitcompat.SplitCompat;
import com.huawei.hms.feature.dynamicinstall.FeatureCompat;

/**
 * Enables immediate access to code and resources of split APK:s installed
 * through {@link com.jeppeman.globallydynamic.globalsplitinstall.GlobalSplitInstallManager}.
 *
 * This class delegates to {@link SplitCompat} and {@link FeatureCompat}
 */
public class GlobalSplitCompat {
    public static boolean install(Context context) {
        return FeatureCompat.install(context) && SplitCompat.install(context);
    }

    public static boolean installActivity(Context context) {
        return FeatureCompat.install(context) && SplitCompat.installActivity(context);
    }

    public static boolean a(Context context) {
        return FeatureCompat.install(context) && SplitCompat.a(context);
    }

    public static boolean a() {
        return SplitCompat.a();
    }
}
