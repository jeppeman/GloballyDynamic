package com.jeppeman.locallydynamic.idea

import com.android.ide.common.gradle.model.IdeAndroidProject
import com.google.common.truth.Truth.assertThat
import com.google.gson.GsonBuilder
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.util.messages.MessageBus
import com.intellij.util.messages.MessageBusConnection
import com.jeppeman.locallydynamic.server.LocalStorageBackend
import com.jeppeman.locallydynamic.server.LocallyDynamicServer
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
class LocallyDynamicServerManagerImplTest {
    @Mock
    private lateinit var mockLogger: LocallyDynamicServerLogger
    @Mock
    private lateinit var mockProject: Project
    @Mock
    private lateinit var mockMessageBus: MessageBus
    @Mock
    private lateinit var mockMessageBusConnection: MessageBusConnection
    @TempDir
    lateinit var tempDir: Path
    private lateinit var locallyDynamicServerManager: LocallyDynamicServerManagerImpl
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
        locallyDynamicServerManager = spy(
            LocallyDynamicServerManagerImpl(
                project = mockProject,
                username = username,
                password = password,
                logger = mockLogger
            )
        )
        projectManagerListener.locallyDynamicServerManager = locallyDynamicServerManager
    }

    @Test
    fun whenServerIsNotRunning_start_shouldStartServerAndWriteConfiguration() {
        val url = "http://locallydynamic.io"
        val mockServer = mock<LocallyDynamicServer> { on { start() } doReturn url }
        val tempBuildFolder = tempDir.resolve("build")
        val mockIdeAndroidProject = mock<IdeAndroidProject> {
            on { buildFolder } doReturn tempBuildFolder.toFile()
        }
        val configurationCaptor = argumentCaptor<LocallyDynamicServer.Configuration>()
        whenever(mockProject.basePath).thenReturn(tempDir.toString())
        doReturn(mockServer).whenever(locallyDynamicServerManager).createServer(any())
        doReturn(listOf(mockIdeAndroidProject)).whenever(locallyDynamicServerManager).getBundleProjects()

        locallyDynamicServerManager.start()

        verify(locallyDynamicServerManager).createServer(configurationCaptor.capture())
        verify(mockServer).start()
        assertThat(configurationCaptor.firstValue.username).isEqualTo(username)
        assertThat(configurationCaptor.firstValue.password).isEqualTo(password)
        assertThat((configurationCaptor.firstValue.storageBackend as LocalStorageBackend).newBuilder().baseStoragePath.toString()).isEqualTo(tempBuildFolder.toString())
        assertThat(configurationCaptor.firstValue.logger).isSameAs(mockLogger)
        assertThat(tempBuildFolder.resolve("locallydynamic/server_info.json").toFile().readText()).isEqualTo(
            gson.toJson(LocallyDynamicServerInfoDto(url, username, password))
        )
    }

    @Test
    fun whenServerIsRunning_start_shouldOnlyWriteConfiguration() {
        val url = "http://locallydynamic.io"
        val mockServer = mock<LocallyDynamicServer> {
            on { address } doReturn url
            on { isRunning } doReturn true
        }
        val tempBuildFolder = tempDir.resolve("build")
        val mockIdeAndroidProject = mock<IdeAndroidProject> {
            on { buildFolder } doReturn tempBuildFolder.toFile()
        }
        locallyDynamicServerManager.server = mockServer
        doReturn(listOf(mockIdeAndroidProject)).whenever(locallyDynamicServerManager).getBundleProjects()

        locallyDynamicServerManager.start()

        verify(locallyDynamicServerManager, never()).createServer(any())
        verify(mockServer, never()).start()
        assertThat(tempBuildFolder.resolve("locallydynamic/server_info.json").toFile().readText()).isEqualTo(
            gson.toJson(LocallyDynamicServerInfoDto(url, username, password))
        )
    }

    @Test
    fun stop_shouldCleanProperly() {
        val tempBuildFolder = tempDir.resolve("build")
        val locallyDynamicFolder = tempBuildFolder.resolve("locallydynamic").apply {
            toFile().mkdirs()
        }
        val serverInfoFile = locallyDynamicFolder.resolve("server_info.json").apply {
            toFile().writeText("content")
        }
        assertThat(serverInfoFile.toFile().exists()).isTrue()
        val mockServer = mock<LocallyDynamicServer>()
        val mockIdeAndroidProject = mock<IdeAndroidProject> {
            on { buildFolder } doReturn tempBuildFolder.toFile()
        }
        doReturn(listOf(mockIdeAndroidProject)).whenever(locallyDynamicServerManager).getBundleProjects()
        locallyDynamicServerManager.server = mockServer

        locallyDynamicServerManager.stop()

        verify(mockServer).stop()
        assertThat(locallyDynamicServerManager.server).isNull()
        assertThat(serverInfoFile.toFile().exists()).isFalse()
    }

    @Test
    fun whenProjectIsClosed_managerShouldStopAndUnregister() {
        whenever(mockProject.name).thenReturn("name")
        whenever(mockProject.locationHash).thenReturn("locationHash")

        projectManagerListener.projectClosed(mockProject)

        verify(locallyDynamicServerManager).stop()
    }

    @Test
    fun whenAnotherProjectIsClosed_managerShouldDoNothing() {
        whenever(mockProject.name).thenReturn("name")

        projectManagerListener.projectClosed(mock())

        verify(locallyDynamicServerManager, never()).stop()
    }
}