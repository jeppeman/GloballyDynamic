package com.jeppeman.globallydynamic.gradle

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
class GloballyDynamicServerInfoTest {
    @TempDir
    lateinit var tempDir: Path
    private lateinit var project: Project

    @BeforeEach
    fun setUp() {
        project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build()
    }

    @Test
    fun whenExtensionHasServerUrl_resolveServerInfo_shouldReturnExtension() {
        val globallyDynamicExtension = GloballyDynamicServer("server").apply {
            serverUrl = "http://"
            username = "username"
            password = "password"
        }

        val serverInfo = project.resolveServerInfo(globallyDynamicExtension)

        assertThat(serverInfo.serverUrl).isEqualTo(globallyDynamicExtension.serverUrl)
        assertThat(serverInfo.username).isEqualTo(globallyDynamicExtension.username)
        assertThat(serverInfo.password).isEqualTo(globallyDynamicExtension.password)
    }

    @Test
    fun whenServerInfoFileExists_resolveServerInfo_shouldDeserializeAndReturnIt() {
        val globallyDynamicServerInfo = GloballyDynamicServerInfoDto(
            serverUrl = "http://server.info",
            username = "u",
            password = "p"
        )
        project.buildDir
            .toPath()
            .resolve("globallydynamic").apply {
                toFile().mkdirs()
            }
            .resolve("server_info.json")
            .toFile()
            .writeText(Gson().toJson(globallyDynamicServerInfo))
        val globallyDynamicExtension = GloballyDynamicServer("server").apply {
            serverUrl = ""
            username = "username"
            password = "password"
        }

        val serverInfo = project.resolveServerInfo(globallyDynamicExtension)

        assertThat(serverInfo.serverUrl).isEqualTo(globallyDynamicServerInfo.serverUrl)
        assertThat(serverInfo.username).isEqualTo(globallyDynamicServerInfo.username)
        assertThat(serverInfo.password).isEqualTo(globallyDynamicServerInfo.password)
    }
}