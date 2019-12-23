package com.jeppeman.globallydynamic.globalsplitcompat;

import android.app.Application;
import android.content.Context;

/**
 * {@link GlobalSplitCompat} capable application
 *
 * @see com.google.android.play.core.splitcompat.SplitCompatApplication
 */
public class GlobalSplitCompatApplication extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        GlobalSplitCompat.install(this);
    }
}
