package com.jeppeman.globallydynamic.globalsplitinstall;


import com.google.android.play.core.splitinstall.SplitInstallRequest;

import org.junit.Test;

import java.util.Locale;

import static com.google.common.truth.Truth.assertThat;

public class GlobalSplitInstallRequestMapperTest {
    @Test
    public void toFeatureInstallRequest_shouldMaintainDataConsistency() {
        GlobalSplitInstallRequest from = GlobalSplitInstallRequest.newBuilder()
                .addLanguage(Locale.FRANCE)
                .addLanguage(Locale.CHINA)
                .addModule("module3")
                .addModule("module5")
                .build();

        SplitInstallRequest featureInstallRequest = GPlayGlobalSplitInstallRequestMapper.toSplitInstallRequest(from);

        assertThat(featureInstallRequest.getLanguages()).isEqualTo(from.getLanguages());
        assertThat(featureInstallRequest.getModuleNames()).isEqualTo(from.getModuleNames());
    }
}
