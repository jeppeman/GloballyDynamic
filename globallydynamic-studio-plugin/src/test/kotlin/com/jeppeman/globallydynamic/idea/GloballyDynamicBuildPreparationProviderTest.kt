package com.jeppeman.globallydynamic.idea

import com.google.common.truth.Truth.assertThat
import com.intellij.execution.configurations.RunConfiguration
import com.nhaarman.mockitokotlin2.*
import org.junit.jupiter.api.Test
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith

@RunWith(JUnitPlatform::class)
class GloballyDynamicBuildPreparationProviderTest {
    private val globallyDynamicBuildPreparationProvider = spy(GloballyDynamicBuildPreparationProvider())

    @Test
    fun whenConfigurationIsNotAndroidRunConfiguration_createTask_shouldReturnNull() {
        val task = globallyDynamicBuildPreparationProvider.createTask(mock())

        assertThat(task).isNull()
    }

    @Test
    fun whenConfigurationIsAndroidRunConfiguration_createTask_shouldCreateTask() {
        val mockRunConfiguration = mock<RunConfiguration>()
        whenever(globallyDynamicBuildPreparationProvider.shouldCreateTask(mockRunConfiguration)).thenReturn(true)

        val task = globallyDynamicBuildPreparationProvider.createTask(mockRunConfiguration)

        assertThat(task).isInstanceOf(GloballyDynamicBuildPreparationTask::class.java)
        assertThat(task!!.isEnabled).isTrue()
    }

    @Test
    fun executeTask_shouldStartServer() {
        val mockServerManager = mock<GloballyDynamicServerManager>()
        doReturn(mockServerManager).whenever(globallyDynamicBuildPreparationProvider).getGloballyDynamicServerManager(any())

        val result = globallyDynamicBuildPreparationProvider.executeTask(
            mock(), mock(), mock(), mock()
        )

        verify(mockServerManager).start()
        assertThat(result).isTrue()
    }
}

