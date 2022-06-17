package com.jeppeman.globallydynamic.globalsplitcompat;

import android.content.Context;

import com.google.android.play.core.splitcompat.SplitCompat;

/**
 * Enables immediate access to code and resources of split APK:s installed
 * through {@link com.jeppeman.globallydynamic.globalsplitinstall.GlobalSplitInstallManager}.
 *
 * This class delegates to {@link SplitCompat}
 */
public class GlobalSplitCompat {
    public static boolean install(Context context) {
        return SplitCompat.install(context);
    }

    public static boolean installActivity(Context context) {
        return SplitCompat.install(context);
    }

    public static boolean a(Context context) {
        return SplitCompat.install(context);
    }

    public static boolean a() {
        return SplitCompat.zze();
    }
}
