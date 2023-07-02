package com.jeppeman.globallydynamic.idea

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project

class StopServerAction : AnAction() {
    fun getServerManager(project: Project?): GloballyDynamicServerManager? =
        project?.globallyDynamicServerManager

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabled = getServerManager(event.project)?.isRunning == true
    }

    override fun actionPerformed(event: AnActionEvent) {
        getServerManager(event.project)?.stop()
    }
}