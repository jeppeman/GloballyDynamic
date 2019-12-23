package com.jeppeman.globallydynamic.idea

import com.android.ide.common.gradle.model.IdeAndroidProject
import com.google.common.truth.Truth.assertThat
import com.google.gson.GsonBuilder
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.util.messages.MessageBus
import com.intellij.util.messages.MessageBusConnection
import com.jeppeman.globallydynamic.server.GloballyDynamicServer
import com.jeppeman.globallydynamic.server.LocalStorageBackend
import com.nhaarman.mockitokotlin2.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.nio.file.Path

@RunWith(JUnitPlatform::class)
@ExtendWith(MockitoExtension::class)
class GloballyDynamicServerManagerImplTest {
    @Mock
    private lateinit var mockLogger: GloballyDynamicServerLogger
    @Mock
    private lateinit var mockProject: Project
    @Mock
    private lateinit var mockMessageBus: MessageBus
    @Mock
    private lateinit var mockMessageBusConnection: MessageBusConnection
    @TempDir
    lateinit var tempDir: Path
    private lateinit var globallyDynamicServerManager: GloballyDynamicServerManagerImpl
    private lateinit var projectManagerListener: AbstractProjectManagerListener
    private val username = "username"
    private val password = "password"
    private val gson = GsonBuilder().disableHtmlEscaping().create()

    @BeforeEach
    fun setUp() {
        whenever(mockMessageBus.connect()).thenReturn(mockMessageBusConnection)
        whenever(mockProject.messageBus).thenReturn(mockMessageBus)
        whenever(mockMessageBusConnection.subscribe(eq(ProjectManager.TOPIC), any())).thenAnswer { invocation ->
            projectManagerListener = invocation.getArgument<AbstractProjectManagerListener>(1)
            Unit
        }
        globallyDynamicServerManager = spy(
            GloballyDynamicServerManagerImpl(
                project = mockProject,
                username = username,
                password = password,
                logger = mockLogger
            )
        )
        projectManagerListener.globallyDynamicServerManager = globallyDynamicServerManager
    }

    @Test
    fun whenServerIsNotRunning_start_shouldStartServerAndWriteConfiguration() {
        val url = "http://globallydynamic.io"
        val mockServer = mock<GloballyDynamicServer> { on { start() } doReturn url }
        val tempBuildFolder = tempDir.resolve("build")
        val mockIdeAndroidProject = mock<IdeAndroidProject> {
            on { buildFolder } doReturn tempBuildFolder.toFile()
        }
        val configurationCaptor = argumentCaptor<GloballyDynamicServer.Configuration>()
        whenever(mockProject.basePath).thenReturn(tempDir.toString())
        doReturn(mockServer).whenever(globallyDynamicServerManager).createServer(any())
        doReturn(listOf(mockIdeAndroidProject)).whenever(globallyDynamicServerManager).getBundleProjects()

        globallyDynamicServerManager.start()

        verify(globallyDynamicServerManager).createServer(configurationCaptor.capture())
        verify(mockServer).start()
        assertThat(configurationCaptor.firstValue.username).isEqualTo(username)
        assertThat(configurationCaptor.firstValue.password).isEqualTo(password)
        assertThat((configurationCaptor.firstValue.storageBackend as LocalStorageBackend).newBuilder().baseStoragePath.toString()).isEqualTo(tempBuildFolder.toString())
        assertThat(configurationCaptor.firstValue.logger).isSameAs(mockLogger)
        assertThat(tempBuildFolder.resolve("globallydynamic/server_info.json").toFile().readText()).isEqualTo(
            gson.toJson(GloballyDynamicServerInfoDto(url, username, password))
        )
    }

    @Test
    fun whenServerIsRunning_start_shouldOnlyWriteConfiguration() {
        val url = "http://globallydynamic.io"
        val mockServer = mock<GloballyDynamicServer> {
            on { address } doReturn url
            on { isRunning } doReturn true
        }
        val tempBuildFolder = tempDir.resolve("build")
        val mockIdeAndroidProject = mock<IdeAndroidProject> {
            on { buildFolder } doReturn tempBuildFolder.toFile()
        }
        globallyDynamicServerManager.server = mockServer
        doReturn(listOf(mockIdeAndroidProject)).whenever(globallyDynamicServerManager).getBundleProjects()

        globallyDynamicServerManager.start()

        verify(globallyDynamicServerManager, never()).createServer(any())
        verify(mockServer, never()).start()
        assertThat(tempBuildFolder.resolve("globallydynamic/server_info.json").toFile().readText()).isEqualTo(
            gson.toJson(GloballyDynamicServerInfoDto(url, username, password))
        )
    }

    @Test
    fun stop_shouldCleanProperly() {
        val tempBuildFolder = tempDir.resolve("build")
        val globallyDynamicFolder = tempBuildFolder.resolve("globallydynamic").apply {
            toFile().mkdirs()
        }
        val serverInfoFile = globallyDynamicFolder.resolve("server_info.json").apply {
            toFile().writeText("content")
        }
        assertThat(serverInfoFile.toFile().exists()).isTrue()
        val mockServer = mock<GloballyDynamicServer>()
        val mockIdeAndroidProject = mock<IdeAndroidProject> {
            on { buildFolder } doReturn tempBuildFolder.toFile()
        }
        doReturn(listOf(mockIdeAndroidProject)).whenever(globallyDynamicServerManager).getBundleProjects()
        globallyDynamicServerManager.server = mockServer

        globallyDynamicServerManager.stop()

        verify(mockServer).stop()
        assertThat(globallyDynamicServerManager.server).isNull()
        assertThat(serverInfoFile.toFile().exists()).isFalse()
    }

    @Test
    fun whenProjectIsClosed_managerShouldStopAndUnregister() {
        whenever(mockProject.name).thenReturn("name")
        whenever(mockProject.locationHash).thenReturn("locationHash")

        projectManagerListener.projectClosed(mockProject)

        verify(globallyDynamicServerManager).stop()
    }

    @Test
    fun whenAnotherProjectIsClosed_managerShouldDoNothing() {
        whenever(mockProject.name).thenReturn("name")

        projectManagerListener.projectClosed(mock())

        verify(globallyDynamicServerManager, never()).stop()
    }
}