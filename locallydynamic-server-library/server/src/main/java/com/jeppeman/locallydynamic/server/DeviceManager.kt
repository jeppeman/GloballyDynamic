package com.jeppeman.locallydynamic.server

import com.android.bundle.Devices
import com.google.gson.Gson
import com.jeppeman.locallydynamic.server.dto.DeviceSpecDto
import com.jeppeman.locallydynamic.server.dto.toDeviceSpec
import java.util.*

internal interface DeviceManager {
    fun registerDevice(deviceSpecDto: DeviceSpecDto): String
    fun getDeviceSpec(deviceId: String): Devices.DeviceSpec?

    companion object : (StorageBackend, Gson) -> DeviceManager {
        override fun invoke(
            storageBackend: StorageBackend,
            gson: Gson
        ): DeviceManager = DeviceManagerImpl(storageBackend, gson)
    }
}

internal class DeviceManagerImpl(
    private val storageBackend: StorageBackend,
    private val gson: Gson
) : DeviceManager {
    override fun registerDevice(deviceSpecDto: DeviceSpecDto): String {
        val deviceId = UUID.randomUUID().toString()
        val deviceSpecJson = gson.toJson(deviceSpecDto)
        storageBackend.storeFile("$deviceId.txt", "application/json", deviceSpecJson.byteInputStream())
        return deviceId
    }

    override fun getDeviceSpec(deviceId: String): Devices.DeviceSpec? {
        val deviceSpecPath = storageBackend.retrieveFile("$deviceId.txt")
        return if (deviceSpecPath != null) {
            val deviceSpecDto = gson.fromJson(deviceSpecPath.toFile().readText(), DeviceSpecDto::class.java)
            storageBackend.deleteFile("$deviceId.txt")
            deviceSpecDto.toDeviceSpec()
        } else {
            null
        }
    }
}