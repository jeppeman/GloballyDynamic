package com.jeppeman.locallydynamic.idea

import com.android.annotations.VisibleForTesting
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.jeppeman.locallydynamic.server.LocallyDynamicServer

class StopServerAction : AnAction() {
    @VisibleForTesting
    fun getServerManager(project: Project?): LocallyDynamicServerManager? =
        project?.let(LocallyDynamicServerManager.Companion::getInstance)

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabled = getServerManager(event.project)?.isRunning == true
    }

    override fun actionPerformed(event: AnActionEvent) {
        getServerManager(event.project)?.stop()
    }
}