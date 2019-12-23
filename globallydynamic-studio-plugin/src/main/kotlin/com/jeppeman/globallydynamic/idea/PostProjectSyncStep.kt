package com.jeppeman.globallydynamic.idea

import com.android.annotations.VisibleForTesting
import com.android.tools.idea.gradle.project.sync.setup.post.ProjectSetupStep
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.jeppeman.globallydynamic.idea.extensions.hasGloballyDynamicEnabled

class PostProjectSyncStep : ProjectSetupStep() {
    @VisibleForTesting
    fun getServerManager(project: Project): GloballyDynamicServerManager =
        GloballyDynamicServerManager.getInstance(project)

    @VisibleForTesting
    fun shouldInitializeServer(project: Project): Boolean =
        ModuleManager.getInstance(project).modules.any(Module::hasGloballyDynamicEnabled)

    override fun setUpProject(project: Project) {
        if (shouldInitializeServer(project)) {
            getServerManager(project).start()
        }
    }
}