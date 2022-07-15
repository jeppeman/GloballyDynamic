package com.jeppeman.globallydynamic.gradle

import com.google.common.truth.Truth.assertThat
import org.gradle.api.Project
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.lang.NumberFormatException

@RunWith(JUnitPlatform::class)
class GloballyDynamicExtensionTest {
    private val globallyDynamicExtension = GloballyDynamicServer("server").apply {
        serverUrl = "serverUrl"
        username = "username"
        password = "password"
        downloadConnectTimeout = 75
        downloadReadTimeout = 25
    }

    @Test
    fun whenPropertyDoesNotExist_resolveServerUrl_shouldFallbackOnValue() {
        val mockProject = mock<Project> { on { properties } doReturn mock() }

        val serverUrl = globallyDynamicExtension.resolveServerUrl(mockProject)

        assertThat(serverUrl).isEqualTo(globallyDynamicExtension.serverUrl)
    }

    @Test
    fun whenPropertyExists_resolveServerUrl_shouldReturnIt() {
        val serverUrlFromProperty = "serverUrlFromProperty"
        val mockProject = mock<Project> {
            on { properties } doReturn mutableMapOf(SERVER_URL_PROPERTY to serverUrlFromProperty)
        }

        val serverUrl = globallyDynamicExtension.resolveServerUrl(mockProject)

        assertThat(serverUrl).isEqualTo(serverUrlFromProperty)
    }

    @Test
    fun whenPropertyDoesNotExist_resolveUsername_shouldFallbackOnValue() {
        val mockProject = mock<Project> { on { properties } doReturn mock() }

        val username = globallyDynamicExtension.resolveUsername(mockProject)

        assertThat(username).isEqualTo(globallyDynamicExtension.username)
    }

    @Test
    fun whenPropertyExists_resolveUsername_shouldReturnIt() {
        val usernameFromProperty = "usernameFromProperty"
        val mockProject = mock<Project> {
            on { properties } doReturn mutableMapOf(USERNAME_PROPERTY to usernameFromProperty)
        }

        val username = globallyDynamicExtension.resolveUsername(mockProject)

        assertThat(username).isEqualTo(usernameFromProperty)
    }

    @Test
    fun whenPropertyDoesNotExist_resolvePassword_shouldFallbackOnValue() {
        val mockProject = mock<Project> { on { properties } doReturn mock() }

        val password = globallyDynamicExtension.resolvePassword(mockProject)

        assertThat(password).isEqualTo(globallyDynamicExtension.password)
    }

    @Test
    fun whenPropertyExists_resolvePassword_shouldReturnIt() {
        val passwordFromProperty = "passwordFromProperty"
        val mockProject = mock<Project> {
            on { properties } doReturn mutableMapOf(PASSWORD_PROPERTY to passwordFromProperty)
        }

        val password = globallyDynamicExtension.resolvePassword(mockProject)

        assertThat(password).isEqualTo(passwordFromProperty)
    }

    @Test
    fun whenPropertyDoesNotExist_resolveThrottleDownloadBy_shouldFallbackOnValue() {
        val mockProject = mock<Project> { on { properties } doReturn mock() }

        val throttle = globallyDynamicExtension.resolveThrottleDownloadBy(mockProject)

        assertThat(throttle).isEqualTo(globallyDynamicExtension.throttleDownloadBy)
    }

    @Test
    fun whenPropertyExists_resolveThrottleDownloadBy_shouldReturnIt() {
        val throttleFromProperty = "5000"
        val mockProject = mock<Project> {
            on { properties } doReturn mutableMapOf(THROTTLE_PROPERTY to throttleFromProperty)
        }

        val throttle = globallyDynamicExtension.resolveThrottleDownloadBy(mockProject)

        assertThat(throttle).isEqualTo(5000)
    }

    @Test
    fun whenPropertyIsNotANumber_resolveThrottleDownloadBy_shouldThrow() {
        val throttleFromProperty = "notANumber"
        val mockProject = mock<Project> {
            on { properties } doReturn mutableMapOf(THROTTLE_PROPERTY to throttleFromProperty)
        }

        val executable = {
            globallyDynamicExtension.resolveThrottleDownloadBy(mockProject)
            Unit
        }

        assertThrows<NumberFormatException>(executable)
    }

    @Test
    fun whenPropertyDoesNotExist_resolveDownloadConnectTimeout_shouldFallbackOnValue() {
        val mockProject = mock<Project> { on { properties } doReturn mock() }

        val throttle = globallyDynamicExtension.resolveDownloadConnectTimeout(mockProject)

        assertThat(throttle).isEqualTo(globallyDynamicExtension.downloadConnectTimeout)
    }

    @Test
    fun whenPropertyExists_resolveDownloadConnectTimeout_shouldReturnIt() {
        val downloadConnectTimeoutProperty = "5000"
        val mockProject = mock<Project> {
            on { properties } doReturn mutableMapOf(DOWNLOAD_CONNECT_TIMEOUT_PROPERTY to downloadConnectTimeoutProperty)
        }

        val throttle = globallyDynamicExtension.resolveDownloadConnectTimeout(mockProject)

        assertThat(throttle).isEqualTo(5000)
    }

    @Test
    fun whenPropertyIsNotANumber_resolveDownloadConnectTimeout_shouldThrow() {
        val downloadConnectTimeoutProperty = "notANumber"
        val mockProject = mock<Project> {
            on { properties } doReturn mutableMapOf(DOWNLOAD_CONNECT_TIMEOUT_PROPERTY to downloadConnectTimeoutProperty)
        }

        val executable = {
            globallyDynamicExtension.resolveDownloadConnectTimeout(mockProject)
            Unit
        }

        assertThrows<NumberFormatException>(executable)
    }

    @Test
    fun whenPropertyDoesNotExist_resolveDownloadReadTimeout_shouldFallbackOnValue() {
        val mockProject = mock<Project> { on { properties } doReturn mock() }

        val throttle = globallyDynamicExtension.resolveDownloadReadTimeout(mockProject)

        assertThat(throttle).isEqualTo(globallyDynamicExtension.downloadReadTimeout)
    }

    @Test
    fun whenPropertyExists_resolveDownloadReadTimeout_shouldReturnIt() {
        val downloadReadTimeoutProperty = "5000"
        val mockProject = mock<Project> {
            on { properties } doReturn mutableMapOf(DOWNLOAD_READ_TIMEOUT_PROPERTY to downloadReadTimeoutProperty)
        }

        val throttle = globallyDynamicExtension.resolveDownloadReadTimeout(mockProject)

        assertThat(throttle).isEqualTo(5000)
    }

    @Test
    fun whenPropertyIsNotANumber_resolveDownloadReadTimeout_shouldThrow() {
        val downloadReadTimeoutProperty = "notANumber"
        val mockProject = mock<Project> {
            on { properties } doReturn mutableMapOf(DOWNLOAD_READ_TIMEOUT_PROPERTY to downloadReadTimeoutProperty)
        }

        val executable = {
            globallyDynamicExtension.resolveDownloadReadTimeout(mockProject)
            Unit
        }

        assertThrows<NumberFormatException>(executable)
    }
}
