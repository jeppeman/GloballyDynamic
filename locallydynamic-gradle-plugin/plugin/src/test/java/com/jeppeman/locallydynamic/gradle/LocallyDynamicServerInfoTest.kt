package com.jeppeman.locallydynamic.gradle

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import java.nio.file.Path

@RunWith(JUnitPlatform::class)
class LocallyDynamicServerInfoTest {
    @TempDir
    lateinit var tempDir: Path
    private lateinit var project: Project

    @BeforeEach
    fun setUp() {
        project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build()
    }

    @Test
    fun whenExtensionHasServerUrl_resolveServerInfo_shouldReturnExtension() {
        val locallyDynamicExtension = LocallyDynamicExtension().apply {
            enabled = true
            serverUrl = "http://"
            username = "username"
            password = "password"
        }

        val serverInfo = project.resolveServerInfo(locallyDynamicExtension)

        assertThat(serverInfo.serverUrl).isEqualTo(locallyDynamicExtension.serverUrl)
        assertThat(serverInfo.username).isEqualTo(locallyDynamicExtension.username)
        assertThat(serverInfo.password).isEqualTo(locallyDynamicExtension.password)
    }

    @Test
    fun whenServerInfoFileExists_resolveServerInfo_shouldDeserializeAndReturnIt() {
        val locallyDynamicServerInfo = LocallyDynamicServerInfo(
            serverUrl = "http://server.info",
            username = "u",
            password = "p"
        )
        project.buildDir
            .toPath()
            .resolve("locallydynamic").apply {
                toFile().mkdirs()
            }
            .resolve("server_info.json")
            .toFile()
            .writeText(Gson().toJson(locallyDynamicServerInfo))
        val locallyDynamicExtension = LocallyDynamicExtension().apply {
            enabled = false
            serverUrl = "http://"
            username = "username"
            password = "password"
        }

        val serverInfo = project.resolveServerInfo(locallyDynamicExtension)

        assertThat(serverInfo.serverUrl).isEqualTo(locallyDynamicServerInfo.serverUrl)
        assertThat(serverInfo.username).isEqualTo(locallyDynamicServerInfo.username)
        assertThat(serverInfo.password).isEqualTo(locallyDynamicServerInfo.password)
    }
}