package com.jeppeman.globallydynamic.globalsplitinstall;

import android.content.Context;

import com.google.android.play.core.splitinstall.SplitInstallHelper;

/**
 * Helper class that enables accessing native libraries installed from splits
 *
 * @see SplitInstallHelper
 */
public class GlobalSplitInstallHelper {
    /**
     * Loads a native library from an installed split
     *
     * @param context the context to load from
     * @param libName the library name
     *
     * @see SplitInstallHelper#loadLibrary(Context, String)
     */
    public static void loadLibrary(Context context, String libName) {
        SplitInstallHelper.loadLibrary(context, libName);
    }

    /**
     * @see SplitInstallHelper#updateAppInfo(Context)
     */
    public static void updateAppInfo(Context context) {
        SplitInstallHelper.updateAppInfo(context);
    }
}
