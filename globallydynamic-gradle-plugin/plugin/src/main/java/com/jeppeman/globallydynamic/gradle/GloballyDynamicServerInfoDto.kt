package com.jeppeman.globallydynamic.gradle

import com.google.gson.GsonBuilder
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import java.io.Serializable
import java.nio.file.Paths

internal data class GloballyDynamicServerInfoDto(
    @get:Input
    val serverUrl: String? = null,
    @get:Input
    val username: String? = null,
    @get:Input
    val password: String? = null
) : Serializable

private fun GloballyDynamicServer.toServerInfo(project: Project): GloballyDynamicServerInfoDto = GloballyDynamicServerInfoDto(
    serverUrl = resolveServerUrl(project),
    username = resolveUsername(project),
    password = resolvePassword(project)
)

/**
 * Tries to resolve the file generated from the Android Studio plugin
 */
private fun Project.resolveServerInfoFromFile(): GloballyDynamicServerInfoDto? {
    val serverInfoFile = Paths.get(
        buildDir.absolutePath,
        "globallydynamic",
        "server_info.json"
    ).toFile()

    return if (serverInfoFile.exists() && serverInfoFile.isFile) {
        try {
            GsonBuilder().disableHtmlEscaping()
                .create()
                .fromJson(serverInfoFile.readText(Charsets.UTF_8), GloballyDynamicServerInfoDto::class.java)
        } catch (exception: Exception) {
            null
        }
    } else {
        null
    }
}

internal fun Project.resolveServerInfo(extension: GloballyDynamicServer): GloballyDynamicServerInfoDto {
    val fromExtension = extension.toServerInfo(project)
    if (fromExtension.serverUrl?.isNotBlank() == true) {
        return fromExtension
    }

    return resolveServerInfoFromFile() ?: GloballyDynamicServerInfoDto()
}