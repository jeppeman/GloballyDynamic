package com.jeppeman.globallydynamic.globalsplitinstall;

import com.huawei.hms.feature.model.FeatureInstallException;

class HuaweiGlobalSplitInstallExceptionFactory {
    static GlobalSplitInstallException create(Exception from) {
        if (from instanceof FeatureInstallException) {
            FeatureInstallException featureInstallException = (FeatureInstallException) from;
            return new GlobalSplitInstallException(featureInstallException.getErrorCode(), from.getMessage(), from);
        } else if (from != null) {
            return new GlobalSplitInstallException(from);
        } else {
            return null;
        }
    }
}
