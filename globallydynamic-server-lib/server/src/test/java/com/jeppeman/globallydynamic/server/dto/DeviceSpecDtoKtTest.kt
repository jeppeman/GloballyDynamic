package com.jeppeman.globallydynamic.server.dto

import com.android.bundle.Devices
import com.google.common.truth.Truth.assertThat
import com.jeppeman.globallydynamic.server.dto.DeviceSpecDto
import com.jeppeman.globallydynamic.server.dto.toDeviceSpec
import com.jeppeman.globallydynamic.server.dto.toDeviceSpecDto
import org.junit.jupiter.api.Test
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith

@RunWith(JUnitPlatform::class)
class DeviceSpecDtoKtTest {
    @Test
    fun toDeviceSpec_shouldMaintainDataConsistency() {
        val deviceSpecDto = DeviceSpecDto(
            supportedAbis = listOf("a", "b", "c"),
            supportedLocales = listOf("aa", "bb", "cc"),
            deviceFeatures = listOf("aaa", "bbb", "ccc"),
            glExtensions = listOf("aaaa", "bbbb", "cccc"),
            screenDensity = 420,
            sdkVersion = 23
        )

        val deviceSpec = deviceSpecDto.toDeviceSpec()

        assertThat(deviceSpec.supportedAbisList).isEqualTo(deviceSpecDto.supportedAbis)
        assertThat(deviceSpec.supportedLocalesList).isEqualTo(deviceSpecDto.supportedLocales)
        assertThat(deviceSpec.deviceFeaturesList).isEqualTo(deviceSpecDto.deviceFeatures)
        assertThat(deviceSpec.glExtensionsList).isEqualTo(deviceSpecDto.glExtensions)
        assertThat(deviceSpec.screenDensity).isEqualTo(deviceSpecDto.screenDensity)
        assertThat(deviceSpec.sdkVersion).isEqualTo(deviceSpecDto.sdkVersion)
    }

    @Test
    fun toDeviceSpecDto_shouldMaintainDataConsistency() {
        val deviceSpec = Devices.DeviceSpec.newBuilder()
            .addAllSupportedAbis(listOf("a", "b", "c"))
            .addAllSupportedLocales(listOf("aa", "bb", "cc"))
            .addAllDeviceFeatures(listOf("aaa", "bbb", "ccc"))
            .addAllGlExtensions(listOf("aaaa", "bbbb", "cccc"))
            .setScreenDensity(420)
            .setSdkVersion(23)
            .build()

        val deviceSpecDto = deviceSpec.toDeviceSpecDto()

        assertThat(deviceSpecDto.supportedAbis).isEqualTo(deviceSpec.supportedAbisList)
        assertThat(deviceSpecDto.supportedLocales).isEqualTo(deviceSpec.supportedLocalesList)
        assertThat(deviceSpecDto.deviceFeatures).isEqualTo(deviceSpec.deviceFeaturesList)
        assertThat(deviceSpecDto.glExtensions).isEqualTo(deviceSpec.glExtensionsList)
        assertThat(deviceSpecDto.screenDensity).isEqualTo(deviceSpec.screenDensity)
        assertThat(deviceSpecDto.sdkVersion).isEqualTo(deviceSpec.sdkVersion)
    }
}