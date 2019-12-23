package com.jeppeman.locallydynamic.gradle

import com.google.gson.GsonBuilder
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import java.io.Serializable
import java.nio.file.Paths

data class LocallyDynamicServerInfo(
    @get:Input
    val serverUrl: String? = null,
    @get:Input
    val username: String? = null,
    @get:Input
    val password: String? = null
) : Serializable

private fun LocallyDynamicExtension.toServerInfo(project: Project): LocallyDynamicServerInfo? = if (enabled) {
    LocallyDynamicServerInfo(
        serverUrl = resolveServerUrl(project),
        username = resolveUsername(project),
        password = resolvePassword(project)
    )
} else {
    null
}

private fun Project.resolveServerInfoFromFile(): LocallyDynamicServerInfo? {
    val serverInfoFile = Paths.get(
        buildDir.absolutePath,
        "locallydynamic",
        "server_info.json"
    ).toFile()

    return if (serverInfoFile.exists() && serverInfoFile.isFile) {
        try {
            GsonBuilder().disableHtmlEscaping()
                .create()
                .fromJson(serverInfoFile.readText(Charsets.UTF_8), LocallyDynamicServerInfo::class.java)
        } catch (exception: Exception) {
            null
        }
    } else {
        null
    }
}

fun Project.resolveServerInfo(extension: LocallyDynamicExtension): LocallyDynamicServerInfo {
    val fromExtension = extension.toServerInfo(project)
    if (fromExtension?.serverUrl?.isNotBlank() == true) {
        return fromExtension
    }

    return resolveServerInfoFromFile() ?: LocallyDynamicServerInfo()
}