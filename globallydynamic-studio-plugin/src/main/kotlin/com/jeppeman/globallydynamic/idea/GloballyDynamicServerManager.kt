package com.jeppeman.globallydynamic.idea

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.intellij.openapi.project.Project
import com.jeppeman.globallydynamic.idea.tooling.globallyDynamicGradle
import com.jeppeman.globallydynamic.server.GloballyDynamicServer
import com.jeppeman.globallydynamic.server.LocalStorageBackend
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

interface GloballyDynamicServerManager {
    val isRunning: Boolean
    val logger: GloballyDynamicServerLogger
    fun start()
    fun stop()
    fun destroy()

    companion object {
        private val managersForProjects = mutableMapOf<Project, GloballyDynamicServerManager>()

        fun unregister(project: Project) {
            managersForProjects.remove(project)
        }

        fun getInstance(project: Project): GloballyDynamicServerManager {
            if (managersForProjects.containsKey(project)) {
                return managersForProjects[project]!!
            }

            managersForProjects[project] = GloballyDynamicServerManagerImpl(project)

            return managersForProjects[project]!!
        }
    }
}

val Project.globallyDynamicServerManager: GloballyDynamicServerManager
    get() = GloballyDynamicServerManager.getInstance(this)

class GloballyDynamicServerManagerImpl(
    private val project: Project,
    private val username: String = UUID.randomUUID().toString().replace("-", ""),
    private val password: String = UUID.randomUUID().toString().replace("-", ""),
    private val gson: Gson = GsonBuilder().disableHtmlEscaping().create(),
    override val logger: GloballyDynamicServerLogger = GloballyDynamicServerLogger()
) : GloballyDynamicServerManager {
    var server: GloballyDynamicServer? = null
    override val isRunning: Boolean get() = server?.isRunning == true

    override fun destroy() {
        stop()
        GloballyDynamicServerManager.unregister(project)
        server = null
    }

    private fun writeServerInfoFiles(address: String) {
        getBundleBuildProjectPaths().forEach { path ->
            writeServerInfoFile(
                globallyDynamicFolder = path,
                serverInfo = GloballyDynamicServerInfoDto(
                    serverUrl = address,
                    username = username,
                    password = password
                )
            )
        }
    }

    private fun writeServerInfoFile(globallyDynamicFolder: Path, serverInfo: GloballyDynamicServerInfoDto) {
        globallyDynamicFolder.toFile().mkdirs()

        val serverInfoPath = globallyDynamicFolder.resolve(SERVER_INFO_FILE)

        Files.deleteIfExists(serverInfoPath)

        serverInfoPath.toFile().writeText(gson.toJson(serverInfo))
    }

    private fun deleteServerInfoFiles() {
        getBundleBuildProjectPaths().forEach(::deleteServerInfoFile)
    }

    private fun deleteServerInfoFile(globallyDynamicFolder: Path) {
        val serverInfoPath = globallyDynamicFolder.resolve(SERVER_INFO_FILE)

        Files.deleteIfExists(serverInfoPath)
    }

    private fun getBundleBuildProjectPaths(): List<Path> = project.globallyDynamicGradle.run {
        gradleProjects
            .filter { it.hasGloballyDynamicEnabled }
            .map { it.buildDirectory.resolve("globallydynamic").toPath() }
    }

    override fun start() {
        if (isRunning) {
            server?.address?.let(::writeServerInfoFiles)
            return
        }

        val newServer = GloballyDynamicServer(
            GloballyDynamicServer.Configuration.builder()
                .setStorageBackend(
                    LocalStorageBackend.builder()
                        .setBaseStoragePath(Paths.get(project.basePath!!, "build"))
                        .build()
                )
                .setUsername(username)
                .setPassword(password)
                .setLogger(logger)
                .setOverrideExistingBundles(true)
                .setValidateSignatureOnDownload(false)
                .build()
        )

        server = newServer

        val address = newServer.start()

        writeServerInfoFiles(address)
    }

    override fun stop() {
        server?.stop()
        server = null
        deleteServerInfoFiles()
    }

    companion object {
        private const val SERVER_INFO_FILE = "server_info.json"
    }
}

class GloballyDynamicServerInfoDto(
    val serverUrl: String,
    val username: String,
    val password: String
)
