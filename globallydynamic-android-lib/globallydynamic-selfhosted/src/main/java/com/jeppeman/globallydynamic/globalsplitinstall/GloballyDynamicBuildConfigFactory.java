package com.jeppeman.globallydynamic.globalsplitinstall;

import com.jeppeman.globallydynamic.generated.GloballyDynamicBuildConfig;

class GloballyDynamicBuildConfigFactory {
    static GloballyDynamicBuildConfig create() {
        try {
            return new GloballyDynamicBuildConfig();
        } catch (NoClassDefFoundError noClassDefFoundError) {
            throw new GloballyDynamicBuildConfigMissingException(
                    "com.jeppeman.globallydynamic.generated.GloballyDynamicBuildConfig not found; " +
                            "make sure that the globallydynamic gradle plugin has been properly applied",
                    noClassDefFoundError
            );
        }
    }
}

