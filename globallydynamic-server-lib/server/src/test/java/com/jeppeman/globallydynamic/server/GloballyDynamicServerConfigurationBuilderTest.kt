package com.jeppeman.globallydynamic.server

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import java.nio.file.Paths

@RunWith(JUnitPlatform::class)
class GloballyDynamicServerConfigurationBuilderTest {
    @Test
    fun whenPathIsRelative_build_shouldAppendItToUserDir() {
        val configuration = GloballyDynamicServer.Configuration.builder()
            .setStorageBackend(LocalStorageBackend.builder().setBaseStoragePath(Paths.get("build")).build())
            .build()

        assertThat((configuration.storageBackend as LocalStorageBackend).newBuilder().baseStoragePath.toString()).isEqualTo(Paths.get(
            System.getProperty("user.dir"),
            "build"
        ).toString())
    }

    @Test
    fun whenPathIsAbsolute_build_shouldNotAppendUserDir() {
        val configuration = GloballyDynamicServer.Configuration.builder()
            .setStorageBackend(LocalStorageBackend.builder().setBaseStoragePath(Paths.get("/build")).build())
            .build()

        assertThat((configuration.storageBackend as LocalStorageBackend).newBuilder().baseStoragePath.toString()).isEqualTo("/build")
    }

    @Test
    fun build_shouldPopulateProperly() {
        val logger = object : Logger {
            override fun i(message: String, newLine: Boolean, prefix: String) = Unit
            override fun e(message: String, newLine: Boolean, prefix: String) = Unit
            override fun e(throwable: Throwable, newLine: Boolean, prefix: String) = Unit
            override fun e(message: String, throwable: Throwable, newLine: Boolean, prefix: String) = Unit
        }

        val configuration = GloballyDynamicServer.Configuration.builder()
            .setPort(888)
            .setUsername("username")
            .setPassword("password")
            .setLogger(logger)
            .build()

        assertThat(configuration.logger).isSameAs(logger)
        assertThat(configuration.port).isEqualTo(888)
        assertThat(configuration.username).isEqualTo("username")
        assertThat(configuration.password).isEqualTo("password")
    }
}