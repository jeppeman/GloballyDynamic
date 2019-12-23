package com.jeppeman.locallydynamic.server

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.jeppeman.locallydynamic.server.dto.DeviceSpecDto
import org.junit.jupiter.api.Test
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith

@RunWith(JUnitPlatform::class)
class DeviceManagerImplTest {
    private val deviceManager = DeviceManagerImpl(
        StorageBackend.LOCAL_DEFAULT,
        Gson()
    )

    @Test
    fun registerDevice_shouldKeepDeviceInMemory() {
        val deviceSpecDto = DeviceSpecDto(
            supportedAbis = listOf("a", "b", "c"),
            supportedLocales = listOf("aa", "bb", "cc"),
            deviceFeatures = listOf("aaa", "bbb", "ccc"),
            glExtensions = listOf("aaaa", "bbbb", "cccc"),
            screenDensity = 420,
            sdkVersion = 23
        )

        val deviceId = deviceManager.registerDevice(deviceSpecDto)
        val deviceSpec = deviceManager.getDeviceSpec(deviceId)

        assertThat(deviceSpec!!.supportedAbisList).isEqualTo(deviceSpecDto.supportedAbis)
        assertThat(deviceSpec.supportedLocalesList).isEqualTo(deviceSpecDto.supportedLocales)
        assertThat(deviceSpec.deviceFeaturesList).isEqualTo(deviceSpecDto.deviceFeatures)
        assertThat(deviceSpec.glExtensionsList).isEqualTo(deviceSpecDto.glExtensions)
        assertThat(deviceSpec.screenDensity).isEqualTo(deviceSpecDto.screenDensity)
        assertThat(deviceSpec.sdkVersion).isEqualTo(deviceSpecDto.sdkVersion)
    }

    @Test
    fun whenDeviceIsNotRegistered_getDeviceSpec_shouldReturnNull() {
        val deviceSpec = deviceManager.getDeviceSpec("deviceId")

        assertThat(deviceSpec).isNull()
    }
}
