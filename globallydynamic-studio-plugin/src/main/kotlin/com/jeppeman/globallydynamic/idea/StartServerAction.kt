package com.jeppeman.globallydynamic.idea

import com.android.annotations.VisibleForTesting
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project

class StartServerAction : AnAction() {
    @VisibleForTesting
    fun getServerManager(project: Project?): GloballyDynamicServerManager? =
        project?.let(GloballyDynamicServerManager.Companion::getInstance)
    
    override fun update(event: AnActionEvent) {
        event.presentation.isEnabled = getServerManager(event.project)?.isRunning != true
    }

    override fun actionPerformed(event: AnActionEvent) {
        getServerManager(event.project)?.start()
    }
}