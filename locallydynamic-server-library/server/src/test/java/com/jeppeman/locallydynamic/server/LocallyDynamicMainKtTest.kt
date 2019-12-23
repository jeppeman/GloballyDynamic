package com.jeppeman.locallydynamic.server

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith

@RunWith(JUnitPlatform::class)
class LocallyDynamicMainKtTest {
    @Test
    fun whenArgumentIsUnrecognized_fromArgs_shouldThrow() {
        val executable = {
            LocallyDynamicServer.Configuration.builder().build().fromArgs(arrayOf("--something"))
            Unit
        }

        assertThrows<IllegalArgumentException>(executable)
    }

    @Test
    fun fromArgs_shouldGenerateConfigurationWithMatchingData() {
        val args = arrayOf(
            "--port", "8096",
            "--storage-backend", "local",
            "--local-storage-path", "/build",
            "--username", "username",
            "--password", "password"
        )

        val configuration = LocallyDynamicServer.Configuration.builder().build().fromArgs(args)

        assertThat(configuration.port).isEqualTo(8096)
        assertThat((configuration.storageBackend as LocalStorageBackend).newBuilder().baseStoragePath.toString()).isEqualTo("/build")
        assertThat(configuration.username).isEqualTo("username")
        assertThat(configuration.password).isEqualTo("password")
    }

    @Test
    fun whenArgumentValueIsMissing_fromArgs_shouldThrow() {
        val args = arrayOf(
            "--port", "8096",
            "--storage-backend", "local",
            "--local-storage-path", "/build",
            "--username", "username",
            "--password"
        )

        val executable = {
            LocallyDynamicServer.Configuration.builder().build().fromArgs(args)
            Unit
        }

        assertThrows<IllegalArgumentException>(executable)
    }

    @Test
    fun whenPortIsNotANumber_fromArgs_shouldThrow() {
        val args = arrayOf(
            "--port", "hej"
        )

        val executable = {
            LocallyDynamicServer.Configuration.builder().build().fromArgs(args)
            Unit
        }

        assertThrows<IllegalArgumentException>(executable)
    }
}