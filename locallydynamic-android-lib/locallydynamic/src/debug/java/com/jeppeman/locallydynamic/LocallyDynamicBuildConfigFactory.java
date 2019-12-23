package com.jeppeman.locallydynamic;

import com.jeppeman.locallydynamic.generated.LocallyDynamicBuildConfig;

class LocallyDynamicBuildConfigFactory {
    static LocallyDynamicBuildConfig create() {
        try {
            return new LocallyDynamicBuildConfig();
        } catch (NoClassDefFoundError noClassDefFoundError) {
            throw new IllegalStateException(
                    "com.jeppeman.locallydynamic.generated.LocallyDynamicBuildConfig not found; " +
                            "make sure that the locallydynamic gradle plugin has been properly applied",
                    noClassDefFoundError
            );
        }
    }
}
