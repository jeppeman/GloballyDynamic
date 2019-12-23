package com.jeppeman.locallydynamic;

import com.google.common.collect.Lists;
import com.jeppeman.locallydynamic.serialization.JsonDeserializerFactory;
import com.jeppeman.locallydynamic.serialization.JsonSerializerFactory;

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

public class LocallyDynamicConfigurationDtoTest {
    @Test
    public void serializationAndDeserialization_shouldLeaveObjectIntact() {
        LocallyDynamicConfigurationDto configurationDto = new LocallyDynamicConfigurationDto(
            "deviceId",
            "serverUrl",
            "username",
            "password",
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
        LocallyDynamicConfigurationDto deserializedConfiguration = JsonDeserializerFactory.create()
            .deserialize(json, LocallyDynamicConfigurationDto.class);

        assertThat(deserializedConfiguration.getDeviceId()).isEqualTo(configurationDto.getDeviceId());
        assertThat(deserializedConfiguration.getServerUrl()).isEqualTo(configurationDto.getServerUrl());
        assertThat(deserializedConfiguration.getUsername()).isEqualTo(configurationDto.getUsername());
        assertThat(deserializedConfiguration.getPassword()).isEqualTo(configurationDto.getPassword());
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
