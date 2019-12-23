package com.jeppeman.globallydynamic.globalsplitinstall;
import com.google.android.play.core.splitinstall.SplitInstallException;
import com.google.android.play.core.splitinstall.model.a;

class GlobalSplitInstallExceptionFactory {
    static GlobalSplitInstallException create(@GlobalSplitInstallErrorCode int errorCode) {
        return new GlobalSplitInstallException(errorCode, a.a(errorCode));
    }


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

    static GlobalSplitInstallException create(
            @GlobalSplitInstallErrorCode int errorCode,
            Throwable throwable) {
        return new GlobalSplitInstallException(errorCode, throwable.getMessage(), throwable);
    }
}
