package com.jeppeman.locallydynamic.idea

import com.android.annotations.VisibleForTesting
import com.android.tools.idea.gradle.project.sync.setup.post.ProjectSetupStep
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.jeppeman.locallydynamic.idea.extensions.hasLocallyDynamicEnabled

class PostProjectSyncStep : ProjectSetupStep() {
    @VisibleForTesting
    fun getServerManager(project: Project): LocallyDynamicServerManager =
        LocallyDynamicServerManager.getInstance(project)

    @VisibleForTesting
    fun shouldInitializeServer(project: Project): Boolean =
        ModuleManager.getInstance(project).modules.any(Module::hasLocallyDynamicEnabled)

    override fun setUpProject(project: Project, progressIndicator: ProgressIndicator?) {
        if (shouldInitializeServer(project)) {
            getServerManager(project).start()
        }
    }
}