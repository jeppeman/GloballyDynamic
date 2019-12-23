package com.jeppeman.globallydynamic.globalsplitinstall;


import com.google.android.play.core.splitinstall.model.a;

public class GlobalSplitInstallErrorCodeHelper {
    public static String getErrorDescription(@GlobalSplitInstallErrorCode int errorCode) {
        return a.a(errorCode);
    }
}
