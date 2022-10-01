package com.jeppeman.globallydynamic.idea

import com.android.annotations.VisibleForTesting
import com.android.tools.idea.gradle.project.facet.gradle.GradleFacet
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ProjectManagerListener
import com.jeppeman.globallydynamic.idea.extensions.hasGloballyDynamicEnabled
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

abstract class AbstractProjectManagerListener(
    @set:VisibleForTesting
    var globallyDynamicServerManager: GloballyDynamicServerManagerImpl
) : ProjectManagerListener {
    override fun projectClosed(project: Project) {
        globallyDynamicServerManager.handleProjectClosed(project)
    }
}

class GloballyDynamicServerManagerImpl(
    private val project: Project,
    private val username: String = UUID.randomUUID().toString().replace("-", ""),
    private val password: String = UUID.randomUUID().toString().replace("-", ""),
    private val gson: Gson = GsonBuilder().disableHtmlEscaping().create(),
    override val logger: GloballyDynamicServerLogger = GloballyDynamicServerLogger()
) : GloballyDynamicServerManager {
    @VisibleForTesting
    var server: GloballyDynamicServer? = null
    override val isRunning: Boolean get() = server?.isRunning == true

    init {
        project.messageBus.connect().subscribe(ProjectManager.TOPIC,
            object : AbstractProjectManagerListener(this) {})
    }

    fun handleProjectClosed(project: Project) {
        if (project.name == this.project.name && project.locationHash == this.project.locationHash) {
            stop()
            GloballyDynamicServerManager.unregister(project)
        }
    }

    private fun writeServerInfoFiles(address: String) {
        getBundleBuildProjectPaths().forEach { path ->
            writeServerInfoFile(
                buildFolderPath = path,
                serverInfo = GloballyDynamicServerInfoDto(
                    serverUrl = address,
                    username = username,
                    password = password
                )
            )
        }
    }

    private fun writeServerInfoFile(buildFolderPath: Path, serverInfo: GloballyDynamicServerInfoDto) {
        val serverInfoDirPath = buildFolderPath.resolve(SERVER_INFO_DIR).apply {
            toFile().mkdirs()
        }

        val serverInfoPath = Paths.get(
            serverInfoDirPath.toString(),
            SERVER_INFO_FILE
        )

        Files.deleteIfExists(serverInfoPath)

        serverInfoPath.toFile().writeText(gson.toJson(serverInfo))
    }

    private fun deleteServerInfoFiles() {
        getBundleBuildProjectPaths().forEach(::deleteServerInfoFile)
    }

    private fun deleteServerInfoFile(buildFolderPath: Path) {
        val serverInfoPath = buildFolderPath.resolve(SERVER_INFO_DIR)
            .resolve(SERVER_INFO_FILE)

        Files.deleteIfExists(serverInfoPath)
    }

    @VisibleForTesting
    fun getBundleBuildProjectPaths(): List<Path> = ModuleManager.getInstance(project)
        ?.modules
        ?.filter { module -> module.hasGloballyDynamicEnabled }
        ?.mapNotNull { module ->
            GradleFacet.getInstance(module)
                ?.gradleModuleModel
                ?.buildFilePath
                ?.parentFile
                ?.resolve("build")
                ?.toPath()
        }
        ?: listOf()

    @VisibleForTesting
    fun createServer(configuration: GloballyDynamicServer.Configuration): GloballyDynamicServer =
        GloballyDynamicServer(configuration)

    override fun start() {
        if (isRunning) {
            server?.address?.let(::writeServerInfoFiles)
            return
        }

        val newServer = createServer(
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
        private const val SERVER_INFO_DIR = "globallydynamic"
        private const val SERVER_INFO_FILE = "server_info.json"
    }
}

class GloballyDynamicServerInfoDto(
    val serverUrl: String,
    val username: String,
    val password: String
)