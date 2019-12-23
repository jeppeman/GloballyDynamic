package com.jeppeman.globallydynamic.gradle

import org.gradle.api.Project

private const val PROPERTY_PREFIX = "globallyDynamicServer"
internal const val SERVER_URL_PROPERTY = "$PROPERTY_PREFIX.serverUrl"
internal const val USERNAME_PROPERTY = "$PROPERTY_PREFIX.username"
internal const val PASSWORD_PROPERTY = "$PROPERTY_PREFIX.password"
internal const val THROTTLE_PROPERTY = "$PROPERTY_PREFIX.throttleDownloadBy"
internal const val UPLOAD_AUTOMATICALLY_PROPERTY = "$PROPERTY_PREFIX.uploadAutomatically"

/**
 * GloballyDynamic server configuration, applied as follows:
 *
 * android {
 *     globallyDynamicServers {
 *         studioIntegrated {
 *            ...
 *         }
 *     }
 * }
 */
open class GloballyDynamicServer(val name: String) {
    /**
     * The amount of time to throttle the downloading of splits by, given in milliseconds
     */
    var throttleDownloadBy: Long = 0
    /**
     * The server url for the GloballyDynamic server
     */
    var serverUrl: String = ""
    /**
     * The username to authenticate with when making requests to the GloballyDynamic server
     */
    var username: String = ""
    /**
     * The password to authenticate with when making requests to the GloballyDynamic server
     */
    var password: String = ""
    /**
     * Whether to disable the dependency check or not (only provided for the sample android project)
     */
    var disableDependencyCheck: Boolean = false
    /**
     * Uploads produced bundles to the server automatically
     */
    var uploadAutomatically: Boolean = true
    /**
     * The build variants for which this server should be applied to
     */
    internal var buildVariants: MutableSet<String> = mutableSetOf()

    fun applyToBuildVariants(vararg buildVariants: String) {
        this.buildVariants = buildVariants.toMutableSet()
    }

    override fun toString(): String = "${this::class.java.simpleName}(" +
        "throttleDownloadBy=$throttleDownloadBy, " +
        "serverUrl=$serverUrl, " +
        "username=$username, " +
        "password=$password," +
        "buildVariants=$buildVariants," +
        ")"
}


internal fun Project.resolveProperty(name: String): String? = properties[name]?.toString()

internal fun GloballyDynamicServer.resolveThrottleDownloadBy(project: Project) =
    project.resolveProperty(THROTTLE_PROPERTY)?.let { throttleBy ->
        try {
            throttleBy.toLong()
        } catch (numberFormatException: NumberFormatException) {
            throw NumberFormatException("Failed to parse $THROTTLE_PROPERTY as Long, got $throttleBy")
        }
    } ?: throttleDownloadBy

internal fun GloballyDynamicServer.resolveServerUrl(project: Project) =
    project.resolveProperty(SERVER_URL_PROPERTY) ?: serverUrl

internal fun GloballyDynamicServer.resolveUsername(project: Project) =
    project.resolveProperty(USERNAME_PROPERTY) ?: username

internal fun GloballyDynamicServer.resolvePassword(project: Project) =
    project.resolveProperty(PASSWORD_PROPERTY) ?: password

internal fun GloballyDynamicServer.resolveUploadAutomatically(project: Project) =
    project.resolveProperty(UPLOAD_AUTOMATICALLY_PROPERTY)?.toBoolean() ?: uploadAutomatically