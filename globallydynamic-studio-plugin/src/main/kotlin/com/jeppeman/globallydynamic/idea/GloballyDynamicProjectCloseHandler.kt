package com.jeppeman.globallydynamic.idea

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectCloseHandler
import com.jeppeman.globallydynamic.idea.tooling.globallyDynamicGradle

class GloballyDynamicProjectCloseHandler : ProjectCloseHandler {
    override fun canClose(project: Project): Boolean {
        project.globallyDynamicGradle.destroy()
        project.globallyDynamicServerManager.destroy()
        return true
    }
}