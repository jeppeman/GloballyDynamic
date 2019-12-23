package com.jeppeman.locallydynamic.gradle

import org.gradle.api.Project

private const val PROPERTY_PREFIX = "locallyDynamic"
internal const val SERVER_URL_PROPERTY = "$PROPERTY_PREFIX.serverUrl"
internal const val USERNAME_PROPERTY = "$PROPERTY_PREFIX.username"
internal const val PASSWORD_PROPERTY = "$PROPERTY_PREFIX.password"
internal const val THROTTLE_PROPERTY = "$PROPERTY_PREFIX.throttleDownloadBy"

/**
 * Dsl extension for the plugin, applied as follows:
 *
 * android {
 *     buildTypes {
 *         debug {
 *             locallyDynamic {
 *                 ...
 *             }
 *         }
 *     }
 * }
 */
open class LocallyDynamicExtension() {
    /**
     * Whether LocallyDynamic should be enabled for the buildType the extension is applied to
     */
    var enabled: Boolean = false
    /**
     * The amount of time to throttle the downloading of splits by, given in milliseconds
     */
    var throttleDownloadBy: Long = 0
    /**
     * The server url for the LocallyDynamic server
     */
    var serverUrl: String = ""
    /**
     * The username to authenticate with when making requests to the LocallyDynamic server
     */
    var username: String = ""
    /**
     * The password to authenticate with when making requests to the LocallyDynamic server
     */
    var password: String = ""

    /**
     * Whether to disable the dependency check or not (only provided for the sample android project)
     */
    var disableDependencyCheck = false

    constructor(enabled: Boolean) : this() {
        this.enabled = enabled
    }

    companion object {
        const val NAME = "locallyDynamic"
    }
}

internal fun Project.resolveProperty(name: String): String? = properties[name]?.toString()

internal fun LocallyDynamicExtension.resolveThrottleDownloadBy(project: Project) =
    project.resolveProperty(THROTTLE_PROPERTY)?.let { throttleBy ->
        try {
            throttleBy.toLong()
        } catch (numberFormatException: NumberFormatException) {
            throw NumberFormatException("Failed to parse $THROTTLE_PROPERTY as Long, got $throttleBy")
        }
    } ?: throttleDownloadBy

internal fun LocallyDynamicExtension.resolveServerUrl(project: Project) =
    project.resolveProperty(SERVER_URL_PROPERTY) ?: serverUrl

internal fun LocallyDynamicExtension.resolveUsername(project: Project) =
    project.resolveProperty(USERNAME_PROPERTY) ?: username

internal fun LocallyDynamicExtension.resolvePassword(project: Project) =
    project.resolveProperty(PASSWORD_PROPERTY) ?: password