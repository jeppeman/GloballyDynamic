package com.jeppeman.globallydynamic.idea

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory

class GloballyDynamicConsoleWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val globallyDynamicServerManager = GloballyDynamicServerManager.getInstance(project)
        val console = GloballyDynamicConsole(
            project = project,
            globallyDynamicLogFormatter = GloballyDynamicLogFormatter(),
            globallyDynamicLogFilterModel = GloballyDynamicLogFilterModel(project),
            globallyDynamicServerManager = globallyDynamicServerManager
        )

        val contentManager = toolWindow.contentManager
        val content = contentManager.factory.createContent(console, "", true)

        contentManager.addContent(content)
        toolWindow.component.add(console.component)
    }
}

//class GloballyDynamicConsoleWindowCondition : Condition<Project> {
//    override fun value(project: Project?): Boolean = false
//        project?.let(ModuleManager::getInstance)
//            ?.modules
//            ?.any(Module::hasGloballyDynamicEnabled) == true
//}