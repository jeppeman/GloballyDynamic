package com.jeppeman.locallydynamic.server

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import java.nio.file.Paths

@RunWith(JUnitPlatform::class)
class LocallyDynamicServerConfigurationBuilderTest {
    @Test
    fun whenPathIsRelative_build_shouldAppendItToUserDir() {
        val configuration = LocallyDynamicServer.Configuration.builder()
            .setStorageBackend(LocalStorageBackend.builder().setBaseStoragePath(Paths.get("build")).build())
            .build()

        assertThat((configuration.storageBackend as LocalStorageBackend).newBuilder().baseStoragePath.toString()).isEqualTo(Paths.get(
            System.getProperty("user.dir"),
            "build"
        ).toString())
    }

    @Test
    fun whenPathIsAbsolute_build_shouldNotAppendUserDir() {
        val configuration = LocallyDynamicServer.Configuration.builder()
            .setStorageBackend(LocalStorageBackend.builder().setBaseStoragePath(Paths.get("/build")).build())
            .build()

        assertThat((configuration.storageBackend as LocalStorageBackend).newBuilder().baseStoragePath.toString()).isEqualTo("/build")
    }

    @Test
    fun build_shouldPopulateProperly() {
        val logger = object : Logger {
            override fun i(message: String) = Unit
            override fun e(message: String) = Unit
            override fun e(throwable: Throwable) = Unit
            override fun e(message: String, throwable: Throwable) = Unit
        }

        val configuration = LocallyDynamicServer.Configuration.builder()
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