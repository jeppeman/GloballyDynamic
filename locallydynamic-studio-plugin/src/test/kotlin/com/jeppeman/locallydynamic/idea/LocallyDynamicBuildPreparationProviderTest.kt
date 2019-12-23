package com.jeppeman.locallydynamic.idea

import com.google.common.truth.Truth.assertThat
import com.intellij.execution.configurations.RunConfiguration
import com.nhaarman.mockitokotlin2.*
import org.junit.jupiter.api.Test
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith

@RunWith(JUnitPlatform::class)
class LocallyDynamicBuildPreparationProviderTest {
    private val locallyDynamicBuildPreparationProvider = spy(LocallyDynamicBuildPreparationProvider())

    @Test
    fun whenConfigurationIsNotAndroidRunConfiguration_createTask_shouldReturnNull() {
        val task = locallyDynamicBuildPreparationProvider.createTask(mock())

        assertThat(task).isNull()
    }

    @Test
    fun whenConfigurationIsAndroidRunConfiguration_createTask_shouldCreateTask() {
        val mockRunConfiguration = mock<RunConfiguration>()
        whenever(locallyDynamicBuildPreparationProvider.shouldCreateTask(mockRunConfiguration)).thenReturn(true)

        val task = locallyDynamicBuildPreparationProvider.createTask(mockRunConfiguration)

        assertThat(task).isInstanceOf(LocallyDynamicBuildPreparationTask::class.java)
        assertThat(task!!.isEnabled).isTrue()
    }

    @Test
    fun executeTask_shouldStartServer() {
        val mockServerManager = mock<LocallyDynamicServerManager>()
        doReturn(mockServerManager).whenever(locallyDynamicBuildPreparationProvider).getLocallyDynamicServerManager(any())

        val result = locallyDynamicBuildPreparationProvider.executeTask(
            mock(), mock(), mock(), mock()
        )

        verify(mockServerManager).start()
        assertThat(result).isTrue()
    }
}

