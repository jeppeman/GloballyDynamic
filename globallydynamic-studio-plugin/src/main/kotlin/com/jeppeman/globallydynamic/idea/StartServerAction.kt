package com.jeppeman.globallydynamic.idea

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class StartServerAction : AnAction() {
    override fun update(event: AnActionEvent) {
        event.presentation.isEnabled = event.project?.globallyDynamicServerManager?.isRunning != true
    }

    override fun actionPerformed(event: AnActionEvent) {
        event.project?.globallyDynamicServerManager?.start()
    }
}