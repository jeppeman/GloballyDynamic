package com.jeppeman.locallydynamic.idea

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Condition
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.jeppeman.locallydynamic.idea.extensions.hasLocallyDynamicEnabled

class LocallyDynamicConsoleWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val locallyDynamicServerManager = LocallyDynamicServerManager.getInstance(project)
        val console = LocallyDynamicConsole(
            project = project,
            locallyDynamicLogFormatter = LocallyDynamicLogFormatter(),
            locallyDynamicLogFilterModel = LocallyDynamicLogFilterModel(project),
            locallyDynamicServerManager = locallyDynamicServerManager
        )

        val contentManager = toolWindow.contentManager
        val content = contentManager.factory.createContent(console, "", true)

        contentManager.addContent(content)
        toolWindow.component.add(console.component)
    }
}

//class LocallyDynamicConsoleWindowCondition : Condition<Project> {
//    override fun value(project: Project?): Boolean = false
//        project?.let(ModuleManager::getInstance)
//            ?.modules
//            ?.any(Module::hasLocallyDynamicEnabled) == true
//}