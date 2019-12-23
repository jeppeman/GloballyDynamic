package com.jeppeman.locallydynamic.idea

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.project.Project
import com.nhaarman.mockitokotlin2.*
import org.junit.jupiter.api.Test
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith

@RunWith(JUnitPlatform::class)
class StopServerActionTest {
    private val stopServerAction: StopServerAction = spy(StopServerAction())

    @Test
    fun whenServerIsRunning_update_shouldEnablePresentation() {
        val mockPresentation = mock<Presentation>()
        val mockProject = mock<Project>()
        val mockServerManager = mock<LocallyDynamicServerManager> { on { isRunning } doReturn true }
        val mockEvent = mock<AnActionEvent> {
            on { presentation } doReturn mockPresentation
            on { project } doReturn mockProject
        }
        doReturn(mockServerManager).whenever(stopServerAction).getServerManager(any())

        stopServerAction.update(mockEvent)

        verify(mockPresentation).isEnabled = true
    }

    @Test
    fun whenServerIsNotRunning_update_shouldDisablePresentation() {
        val mockPresentation = mock<Presentation>()
        val mockEvent = mock<AnActionEvent> { on { presentation } doReturn mockPresentation }

        stopServerAction.update(mockEvent)

        verify(mockPresentation).isEnabled = false
    }
}