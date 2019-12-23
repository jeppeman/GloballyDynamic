package com.jeppeman.globallydynamic.globalsplitinstall;

import com.google.android.play.core.splitinstall.SplitInstallException;

class GPlayGlobalSplitInstallExceptionFactory {
    static GlobalSplitInstallException create(Exception from) {
        if (from instanceof SplitInstallException) {
            SplitInstallException splitInstallException = (SplitInstallException) from;
            return new GlobalSplitInstallException(splitInstallException.getErrorCode(), from.getMessage(), from);
        } else if (from != null) {
            return new GlobalSplitInstallException(from);
        } else {
            return null;
        }
    }
}
