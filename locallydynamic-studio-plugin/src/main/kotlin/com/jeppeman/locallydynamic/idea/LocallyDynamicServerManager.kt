package com.jeppeman.locallydynamic.idea

import com.android.annotations.VisibleForTesting
import com.android.ide.common.gradle.model.IdeAndroidProject
import com.android.tools.idea.gradle.project.model.AndroidModuleModel
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ProjectManagerListener
import com.jeppeman.locallydynamic.idea.extensions.hasLocallyDynamicEnabled
import com.jeppeman.locallydynamic.server.LocalStorageBackend
import com.jeppeman.locallydynamic.server.LocallyDynamicServer
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

interface LocallyDynamicServerManager {
    val isRunning: Boolean
    val logger: LocallyDynamicServerLogger
    fun start()
    fun stop()

    companion object {
        private val managersForProjects = mutableMapOf<Project, LocallyDynamicServerManager>()

        fun unregister(project: Project) {
            managersForProjects.remove(project)
        }

        fun getInstance(project: Project): LocallyDynamicServerManager {
            if (managersForProjects.containsKey(project)) {
                return managersForProjects[project]!!
            }

            managersForProjects[project] = LocallyDynamicServerManagerImpl(project)

            return managersForProjects[project]!!
        }
    }
}

abstract class AbstractProjectManagerListener(
    @set:VisibleForTesting
    var locallyDynamicServerManager: LocallyDynamicServerManagerImpl
) : ProjectManagerListener {
    override fun projectClosed(project: Project) {
        locallyDynamicServerManager.handleProjectClosed(project)
    }
}

class LocallyDynamicServerManagerImpl(
    private val project: Project,
    private val username: String = UUID.randomUUID().toString().replace("-", ""),
    private val password: String = UUID.randomUUID().toString().replace("-", ""),
    private val gson: Gson = GsonBuilder().disableHtmlEscaping().create(),
    override val logger: LocallyDynamicServerLogger = LocallyDynamicServerLogger()
) : LocallyDynamicServerManager {
    @VisibleForTesting
    var server: LocallyDynamicServer? = null
    override val isRunning: Boolean get() = server?.isRunning == true

    init {
        project.messageBus.connect().subscribe(ProjectManager.TOPIC,
            object : AbstractProjectManagerListener(this) {})
    }

    fun handleProjectClosed(project: Project) {
        if (project.name == this.project.name && project.locationHash == this.project.locationHash) {
            stop()
            LocallyDynamicServerManager.unregister(project)
        }
    }

    private fun writeServerInfoFiles(address: String) {
        getBundleProjects().forEach { androidProject ->
            writeServerInfoFile(
                androidProject = androidProject,
                serverInfo = LocallyDynamicServerInfoDto(
                    serverUrl = address,
                    username = username,
                    password = password
                )
            )
        }
    }

    private fun writeServerInfoFile(androidProject: IdeAndroidProject, serverInfo: LocallyDynamicServerInfoDto) {
        val serverInfoDirPath = Paths.get(
            androidProject.buildFolder.absolutePath,
            SERVER_INFO_DIR
        ).apply {
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
        getBundleProjects().forEach(::deleteServerInfoFile)
    }

    private fun deleteServerInfoFile(androidProject: IdeAndroidProject) {
        val serverInfoPath = Paths.get(
            androidProject.buildFolder.absolutePath,
            SERVER_INFO_DIR,
            SERVER_INFO_FILE
        )

        Files.deleteIfExists(serverInfoPath)
    }

    @VisibleForTesting
    fun getBundleProjects(): List<IdeAndroidProject> = ModuleManager.getInstance(project)
        ?.modules
        ?.filter { module -> module.hasLocallyDynamicEnabled }
        ?.mapNotNull { module -> AndroidModuleModel.get(module)?.androidProject }
        ?: listOf()

    @VisibleForTesting
    fun createServer(configuration: LocallyDynamicServer.Configuration): LocallyDynamicServer =
        LocallyDynamicServer(configuration)

    override fun start() {
        if (isRunning) {
            server?.address?.let(::writeServerInfoFiles)
            return
        }

        val newServer = createServer(
            LocallyDynamicServer.Configuration.builder()
                .setStorageBackend(
                    LocalStorageBackend.builder()
                        .setBaseStoragePath(Paths.get(project.basePath!!, "build"))
                        .build()
                )
                .setUsername(username)
                .setPassword(password)
                .setLogger(logger)
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
        private const val SERVER_INFO_DIR = "locallydynamic"
        private const val SERVER_INFO_FILE = "server_info.json"
    }
}

class LocallyDynamicServerInfoDto(
    val serverUrl: String,
    val username: String,
    val password: String
)