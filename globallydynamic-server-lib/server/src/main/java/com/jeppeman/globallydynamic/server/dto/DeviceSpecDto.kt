package com.jeppeman.globallydynamic.server.dto

import com.android.bundle.Devices

internal data class DeviceSpecDto(
    val supportedAbis: List<String>,
    val supportedLocales: List<String>,
    val deviceFeatures: List<String>,
    val glExtensions: List<String>,
    val screenDensity: Int,
    val sdkVersion: Int
)

internal fun DeviceSpecDto.toDeviceSpec(): Devices.DeviceSpec = Devices.DeviceSpec.newBuilder()
    .addAllSupportedAbis(supportedAbis)
    .addAllSupportedLocales(supportedLocales)
    .addAllDeviceFeatures(deviceFeatures)
    .addAllGlExtensions(glExtensions)
    .setScreenDensity(screenDensity)
    .setSdkVersion(sdkVersion)
    .build()

internal fun Devices.DeviceSpec.toDeviceSpecDto(): DeviceSpecDto = DeviceSpecDto(
    supportedAbis = supportedAbisList,
    supportedLocales = supportedLocalesList,
    deviceFeatures = deviceFeaturesList,
    glExtensions = glExtensionsList,
    screenDensity = screenDensity,
    sdkVersion = sdkVersion
)