package com.jeppeman.globallydynamic.globalsplitinstall;

import com.google.common.collect.Lists;
import com.jeppeman.globallydynamic.serialization.JsonDeserializerFactory;
import com.jeppeman.globallydynamic.serialization.JsonSerializerFactory;

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

public class GloballyDynamicConfigurationDtoTest {
    @Test
    public void serializationAndDeserialization_shouldLeaveObjectIntact() {
        GloballyDynamicConfigurationDto configurationDto = new GloballyDynamicConfigurationDto(
            "serverUrl",
            "applicationId",
            "variantName",
            224,
            5000,
            new DeviceSpecDto(
                Lists.newArrayList("x86", "armeabi", "armeabi-v7a"),
                Lists.newArrayList("sv", "se", "en"),
                Lists.newArrayList("a", "b", "c", "d", "e", "f", "g"),
                Lists.newArrayList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k"),
                420,
                23
            )
        );

        String json = JsonSerializerFactory.create().serialize(configurationDto);
        GloballyDynamicConfigurationDto deserializedConfiguration = JsonDeserializerFactory.create()
            .deserialize(json, GloballyDynamicConfigurationDto.class);

        assertThat(deserializedConfiguration.getServerUrl()).isEqualTo(configurationDto.getServerUrl());
        assertThat(deserializedConfiguration.getApplicationId()).isEqualTo(configurationDto.getApplicationId());
        assertThat(deserializedConfiguration.getVariantName()).isEqualTo(configurationDto.getVariantName());
        assertThat(deserializedConfiguration.getVersionCode()).isEqualTo(configurationDto.getVersionCode());
        assertThat(deserializedConfiguration.getThrottleDownloadBy()).isEqualTo(configurationDto.getThrottleDownloadBy());
        assertThat(deserializedConfiguration.getDeviceSpec().getSupportedAbis()).isEqualTo(configurationDto.getDeviceSpec().getSupportedAbis());
        assertThat(deserializedConfiguration.getDeviceSpec().getSupportedLocales()).isEqualTo(configurationDto.getDeviceSpec().getSupportedLocales());
        assertThat(deserializedConfiguration.getDeviceSpec().getDeviceFeatures()).isEqualTo(configurationDto.getDeviceSpec().getDeviceFeatures());
        assertThat(deserializedConfiguration.getDeviceSpec().getGlExtensions()).isEqualTo(configurationDto.getDeviceSpec().getGlExtensions());
        assertThat(deserializedConfiguration.getDeviceSpec().getScreenDensity()).isEqualTo(configurationDto.getDeviceSpec().getScreenDensity());
        assertThat(deserializedConfiguration.getDeviceSpec().getSdkVersion()).isEqualTo(configurationDto.getDeviceSpec().getSdkVersion());
    }
}
