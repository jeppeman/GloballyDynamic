package com.jeppeman.locallydynamic.idea

import com.intellij.openapi.project.Project
import com.nhaarman.mockitokotlin2.*
import org.junit.jupiter.api.Test
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith

@RunWith(JUnitPlatform::class)
class PostProjectSyncStepTest {
    private val postProjectSyncStep: PostProjectSyncStep = spy(PostProjectSyncStep())

    @Test
    fun whenServerShouldBeInitialized_setUpProject_shouldStartServer() {
        val mockProject = mock<Project>()
        val mockServerManager = mock<LocallyDynamicServerManager>()
        doReturn(true).whenever(postProjectSyncStep).shouldInitializeServer(mockProject)
        doReturn(mockServerManager).whenever(postProjectSyncStep).getServerManager(mockProject)

        postProjectSyncStep.setUpProject(mockProject, null)

        verify(mockServerManager).start()
    }

    @Test
    fun whenServerShouldNotBeInitialized_setUpProject_shouldNotStartServer() {
        val mockProject = mock<Project>()
        val mockServerManager = mock<LocallyDynamicServerManager>()
        doReturn(false).whenever(postProjectSyncStep).shouldInitializeServer(mockProject)
        doReturn(mockServerManager).whenever(postProjectSyncStep).getServerManager(mockProject)

        postProjectSyncStep.setUpProject(mockProject, null)

        verify(mockServerManager, never()).start()
    }
}