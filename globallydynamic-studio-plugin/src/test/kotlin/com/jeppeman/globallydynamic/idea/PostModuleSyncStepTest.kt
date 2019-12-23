package com.jeppeman.globallydynamic.idea

import com.google.common.truth.Truth.assertThat
import com.intellij.execution.BeforeRunTask
import com.intellij.execution.RunManagerEx
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.nhaarman.mockitokotlin2.*
import org.junit.jupiter.api.Test
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith

@RunWith(JUnitPlatform::class)
class PostModuleSyncStepTest {
    private val postModuleSyncStep = spy(PostModuleSyncStep())

    @Test
    fun whenModuleDoesNotHaveGloballyDynamicTasks_setupModule_shouldNotDoAnything() {
        val mockModule = mock<Module> { on { isDisposed} doReturn true }
        doReturn(false).whenever(postModuleSyncStep).shouldAddBuildPreparationTask(mockModule)

        postModuleSyncStep.setUpModule(mockModule, null)

        verify(postModuleSyncStep, never()).getRunManagerX(any())
        verify(postModuleSyncStep, never()).getAndroidRunConfigurations(any())
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
        doReturn(mockRunManagerEx).whenever(postModuleSyncStep).getRunManagerX(mockProject)
        doReturn(true).whenever(postModuleSyncStep).shouldAddBuildPreparationTask(mockModule)
        doReturn(listOf(mockAndroidRunConfiguration)).whenever(postModuleSyncStep).getAndroidRunConfigurations(mockRunManagerEx)

        postModuleSyncStep.setUpModule(mockModule, null)

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
        doReturn(mockRunManagerEx).whenever(postModuleSyncStep).getRunManagerX(mockProject)
        doReturn(mockGloballyDynamicBuildPreparationProvider).whenever(postModuleSyncStep).getGloballyDynamicBuildPreparationProvider()
        doReturn(listOf(mockAndroidRunConfiguration)).whenever(postModuleSyncStep).getAndroidRunConfigurations(mockRunManagerEx)
        doReturn(true).whenever(postModuleSyncStep).shouldAddBuildPreparationTask(mockModule)

        postModuleSyncStep.setUpModule(mockModule, null)

        verify(mockRunManagerEx).setBeforeRunTasks(eq(mockAndroidRunConfiguration), beforeRunTasksCaptor.capture())
        assertThat(beforeRunTasksCaptor.firstValue.first()).isSameAs(mockGloballyDynamicBuildPreparationTask)
        assertThat(beforeRunTasksCaptor.firstValue.filterIsInstance<GloballyDynamicBuildPreparationTask>()).hasSize(1)
    }
}