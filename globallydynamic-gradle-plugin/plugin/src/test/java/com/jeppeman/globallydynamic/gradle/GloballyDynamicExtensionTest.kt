package com.jeppeman.globallydynamic.gradle

import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import org.gradle.api.Project
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import java.lang.NumberFormatException

@RunWith(JUnitPlatform::class)
class GloballyDynamicExtensionTest {
    private val globallyDynamicExtension = GloballyDynamicServer("server").apply {
        serverUrl = "serverUrl"
        username = "username"
        password = "password"
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
}
