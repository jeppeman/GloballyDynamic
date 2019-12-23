package com.jeppeman.locallydynamic.gradle

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
class LocallyDynamicExtensionTest {
    private val locallyDynamicExtension = LocallyDynamicExtension().apply {
        enabled = true
        serverUrl = "serverUrl"
        username = "username"
        password = "password"
    }

    @Test
    fun whenPropertyDoesNotExist_resolveServerUrl_shouldFallbackOnValue() {
        val mockProject = mock<Project> { on { properties } doReturn mock() }

        val serverUrl = locallyDynamicExtension.resolveServerUrl(mockProject)

        assertThat(serverUrl).isEqualTo(locallyDynamicExtension.serverUrl)
    }

    @Test
    fun whenPropertyExists_resolveServerUrl_shouldReturnIt() {
        val serverUrlFromProperty = "serverUrlFromProperty"
        val mockProject = mock<Project> {
            on { properties } doReturn mutableMapOf(SERVER_URL_PROPERTY to serverUrlFromProperty)
        }

        val serverUrl = locallyDynamicExtension.resolveServerUrl(mockProject)

        assertThat(serverUrl).isEqualTo(serverUrlFromProperty)
    }

    @Test
    fun whenPropertyDoesNotExist_resolveUsername_shouldFallbackOnValue() {
        val mockProject = mock<Project> { on { properties } doReturn mock() }

        val username = locallyDynamicExtension.resolveUsername(mockProject)

        assertThat(username).isEqualTo(locallyDynamicExtension.username)
    }

    @Test
    fun whenPropertyExists_resolveUsername_shouldReturnIt() {
        val usernameFromProperty = "usernameFromProperty"
        val mockProject = mock<Project> {
            on { properties } doReturn mutableMapOf(USERNAME_PROPERTY to usernameFromProperty)
        }

        val username = locallyDynamicExtension.resolveUsername(mockProject)

        assertThat(username).isEqualTo(usernameFromProperty)
    }

    @Test
    fun whenPropertyDoesNotExist_resolvePassword_shouldFallbackOnValue() {
        val mockProject = mock<Project> { on { properties } doReturn mock() }

        val password = locallyDynamicExtension.resolvePassword(mockProject)

        assertThat(password).isEqualTo(locallyDynamicExtension.password)
    }

    @Test
    fun whenPropertyExists_resolvePassword_shouldReturnIt() {
        val passwordFromProperty = "passwordFromProperty"
        val mockProject = mock<Project> {
            on { properties } doReturn mutableMapOf(PASSWORD_PROPERTY to passwordFromProperty)
        }

        val password = locallyDynamicExtension.resolvePassword(mockProject)

        assertThat(password).isEqualTo(passwordFromProperty)
    }

    @Test
    fun whenPropertyDoesNotExist_resolveThrottleDownloadBy_shouldFallbackOnValue() {
        val mockProject = mock<Project> { on { properties } doReturn mock() }

        val throttle = locallyDynamicExtension.resolveThrottleDownloadBy(mockProject)

        assertThat(throttle).isEqualTo(locallyDynamicExtension.throttleDownloadBy)
    }

    @Test
    fun whenPropertyExists_resolveThrottleDownloadBy_shouldReturnIt() {
        val throttleFromProperty = "5000"
        val mockProject = mock<Project> {
            on { properties } doReturn mutableMapOf(THROTTLE_PROPERTY to throttleFromProperty)
        }

        val throttle = locallyDynamicExtension.resolveThrottleDownloadBy(mockProject)

        assertThat(throttle).isEqualTo(5000)
    }

    @Test
    fun whenPropertyIsNotANumber_resolveThrottleDownloadBy_shouldThrow() {
        val throttleFromProperty = "notANumber"
        val mockProject = mock<Project> {
            on { properties } doReturn mutableMapOf(THROTTLE_PROPERTY to throttleFromProperty)
        }

        val executable = {
            locallyDynamicExtension.resolveThrottleDownloadBy(mockProject)
            Unit
        }

        assertThrows<NumberFormatException>(executable)
    }
}
