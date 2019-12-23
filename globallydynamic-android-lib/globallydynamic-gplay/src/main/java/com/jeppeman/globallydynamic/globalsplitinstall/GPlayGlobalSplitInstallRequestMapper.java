package com.jeppeman.globallydynamic.globalsplitinstall;

import com.google.android.play.core.splitinstall.SplitInstallRequest;

import java.util.Locale;

class GPlayGlobalSplitInstallRequestMapper {
    static SplitInstallRequest toSplitInstallRequest(GlobalSplitInstallRequest from) {
        SplitInstallRequest.Builder builder = SplitInstallRequest.newBuilder();

        for (Locale locale : from.getLanguages()) {
            builder.addLanguage(locale);
        }

        for (String module : from.getModuleNames()) {
            builder.addModule(module);
        }

        return builder.build();
    }
}
