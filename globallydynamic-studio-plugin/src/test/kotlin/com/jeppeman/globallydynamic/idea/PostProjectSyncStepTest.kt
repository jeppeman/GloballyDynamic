package com.jeppeman.globallydynamic.idea

import com.google.common.truth.Truth.assertThat
import com.intellij.execution.BeforeRunTask
import com.intellij.execution.RunManagerEx
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.nhaarman.mockitokotlin2.*
import org.junit.jupiter.api.*
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith

@RunWith(JUnitPlatform::class)
class PostProjectSyncStepTest {
    private val mockModuleManager: ModuleManager = mock()
    private val postProjectSyncStep: PostProjectSyncStep = spy(PostProjectSyncStep())

    @BeforeEach
    fun setUp() {
        doReturn(mockModuleManager).whenever(postProjectSyncStep).getModuleManager(any())
        doReturn(arrayOf<Module>()).whenever(mockModuleManager).modules
    }

    @Test
    fun whenServerShouldBeInitialized_setUpProject_shouldStartServer() {
        val mockProject = mock<Project>()
        val mockServerManager = mock<GloballyDynamicServerManager>()
        doReturn(true).whenever(postProjectSyncStep).shouldInitializeServer(mockProject)
        doReturn(mockServerManager).whenever(postProjectSyncStep).getServerManager(mockProject)

        postProjectSyncStep.setUpProject(mockProject)

        verify(mockServerManager).start()
    }

    @Test
    fun whenServerShouldNotBeInitialized_setUpProject_shouldNotStartServer() {
        val mockProject = mock<Project>()
        val mockServerManager = mock<GloballyDynamicServerManager>()
        doReturn(false).whenever(postProjectSyncStep).shouldInitializeServer(mockProject)
        doReturn(mockServerManager).whenever(postProjectSyncStep).getServerManager(mockProject)

        postProjectSyncStep.setUpProject(mockProject)

        verify(mockServerManager, never()).start()
    }

    @Test
    fun whenModuleDoesNotHaveGloballyDynamicTasks_setupModule_shouldNotDoAnything() {
        val mockModule = mock<Module> { on { isDisposed} doReturn true }
        doReturn(false).whenever(postProjectSyncStep).shouldInitializeServer(any())
        doReturn(arrayOf(mockModule)).whenever(mockModuleManager).modules
        doReturn(false).whenever(postProjectSyncStep).shouldAddBuildPreparationTask(mockModule)

        postProjectSyncStep.setUpProject(mock())

        verify(postProjectSyncStep, never()).getRunManagerX(any())
        verify(postProjectSyncStep, never()).getAndroidRunConfigurations(any())
    }

    @Test
    fun whenPreparationTaskExists_setupModule_shouldReorderItToFront() {
        val mockProject = mock<Project>()
        val mockModule = mock<Module> {
            on { project } doReturn mockProject
            on { isDisposed } doReturn true
        }
        val beforeRunTasksCaptor = argumentCaptor<List<BeforeRunTask<*>>>()
        val mockAndroidRunConfiguration = mock<RunConfiguration>()
        val preparationTask = GloballyDynamicBuildPreparationTask()
        val mockRunManagerEx = mock<RunManagerEx> {
            on { getBeforeRunTasks(mockAndroidRunConfiguration) } doReturn listOf(mock<BeforeRunTask<*>>(), mock<BeforeRunTask<*>>(), preparationTask)
        }
        doReturn(false).whenever(postProjectSyncStep).shouldInitializeServer(mockProject)
        doReturn(arrayOf(mockModule)).whenever(mockModuleManager).modules
        doReturn(mockRunManagerEx).whenever(postProjectSyncStep).getRunManagerX(mockProject)
        doReturn(true).whenever(postProjectSyncStep).shouldAddBuildPreparationTask(mockModule)
        doReturn(listOf(mockAndroidRunConfiguration)).whenever(postProjectSyncStep).getAndroidRunConfigurations(mockRunManagerEx)

        postProjectSyncStep.setUpProject(mockProject)

        verify(mockRunManagerEx).setBeforeRunTasks(eq(mockAndroidRunConfiguration), beforeRunTasksCaptor.capture())
        assertThat(beforeRunTasksCaptor.firstValue.first()).isSameAs(preparationTask)
        assertThat(beforeRunTasksCaptor.firstValue.filterIsInstance<GloballyDynamicBuildPreparationTask>()).hasSize(1)
    }

    @Test
    fun whenPreparationTaskDoesNotExist_setupModule_shouldCreateItInFront() {
        val mockProject = mock<Project>()
        val mockModule = mock<Module> {
            on { project } doReturn mockProject
            on { isDisposed } doReturn true
        }
        val mockGloballyDynamicBuildPreparationTask = mock<GloballyDynamicBuildPreparationTask>()
        val mockGloballyDynamicBuildPreparationProvider = mock<GloballyDynamicBuildPreparationProvider> {
            on { createTask(any())} doReturn mockGloballyDynamicBuildPreparationTask
        }
        val beforeRunTasksCaptor = argumentCaptor<List<BeforeRunTask<*>>>()
        val mockAndroidRunConfiguration = mock<RunConfiguration>()
        val mockRunManagerEx = mock<RunManagerEx> {
            on { getBeforeRunTasks(mockAndroidRunConfiguration) } doReturn mutableListOf(mock<BeforeRunTask<*>>(), mock<BeforeRunTask<*>>()) as List<BeforeRunTask<*>>
        }
        doReturn(false).whenever(postProjectSyncStep).shouldInitializeServer(mockProject)
        doReturn(arrayOf(mockModule)).whenever(mockModuleManager).modules
        doReturn(mockRunManagerEx).whenever(postProjectSyncStep).getRunManagerX(mockProject)
        doReturn(mockGloballyDynamicBuildPreparationProvider).whenever(postProjectSyncStep).getGloballyDynamicBuildPreparationProvider()
        doReturn(listOf(mockAndroidRunConfiguration)).whenever(postProjectSyncStep).getAndroidRunConfigurations(mockRunManagerEx)
        doReturn(true).whenever(postProjectSyncStep).shouldAddBuildPreparationTask(mockModule)

        postProjectSyncStep.setUpProject(mockProject)

        verify(mockRunManagerEx).setBeforeRunTasks(eq(mockAndroidRunConfiguration), beforeRunTasksCaptor.capture())
        assertThat(beforeRunTasksCaptor.firstValue.first()).isSameAs(mockGloballyDynamicBuildPreparationTask)
        assertThat(beforeRunTasksCaptor.firstValue.filterIsInstance<GloballyDynamicBuildPreparationTask>()).hasSize(1)
    }
}