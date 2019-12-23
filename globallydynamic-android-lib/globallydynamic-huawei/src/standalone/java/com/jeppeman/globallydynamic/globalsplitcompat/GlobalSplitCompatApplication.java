package com.jeppeman.globallydynamic.globalsplitcompat;

import android.app.Application;
import android.content.Context;

/**
 * {@link GlobalSplitCompatApplication} capable application
 */
public class GlobalSplitCompatApplication extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        GlobalSplitCompat.install(this);
    }
}
