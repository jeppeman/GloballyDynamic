package com.jeppeman.globallydynamic.globalsplitinstall;

import com.huawei.hms.feature.model.FeatureInstallRequest;

import java.util.Locale;

class HuaweiGlobalSplitInstallRequestMapper {
    static FeatureInstallRequest toFeatureInstallRequest(GlobalSplitInstallRequest from) {
        FeatureInstallRequest.Builder builder = FeatureInstallRequest.newBuilder();

        for (Locale locale : from.getLanguages()) {
            builder.addLanguage(locale);
        }

        for (String module : from.getModuleNames()) {
            builder.addModule(module);
        }

        return builder.build();
    }
}
