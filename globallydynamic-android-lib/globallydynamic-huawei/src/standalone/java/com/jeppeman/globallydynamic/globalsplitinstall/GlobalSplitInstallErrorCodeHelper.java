package com.jeppeman.globallydynamic.globalsplitinstall;


import com.huawei.hms.feature.model.FeatureInstallErrorCode;

public class GlobalSplitInstallErrorCodeHelper {
    public static String getErrorDescription(@GlobalSplitInstallErrorCode int errorCode) {
        return FeatureInstallErrorCode.ERROR_INFO.get(errorCode);
    }
}
