package com.jeppeman.globallydynamic.idea

import com.android.annotations.VisibleForTesting
import com.android.tools.idea.gradle.project.sync.setup.post.ModuleSetupStep
import com.android.tools.idea.run.AndroidRunConfigurationBase
import com.intellij.execution.RunManagerEx
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.jeppeman.globallydynamic.idea.extensions.hasGloballyDynamicEnabled

class PostModuleSyncStep : ModuleSetupStep() {
    @VisibleForTesting
    fun getRunManagerX(project: Project): RunManagerEx = RunManagerEx.getInstanceEx(project)

    @VisibleForTesting
    fun getAndroidRunConfigurations(runManagerEx: RunManagerEx): List<RunConfiguration> =
        runManagerEx.allConfigurationsList.filterIsInstance<AndroidRunConfigurationBase>()
    @VisibleForTesting
    fun getGloballyDynamicBuildPreparationProvider() = GloballyDynamicBuildPreparationProvider()

    @VisibleForTesting
    fun shouldAddBuildPreparationTask(module: Module): Boolean = module.hasGloballyDynamicEnabled

    override fun setUpModule(module: Module, progressIndicator: ProgressIndicator?) {
        if (shouldAddBuildPreparationTask(module)) {
            val runManagerEx = getRunManagerX(module.project)
            val androidRunConfigurations = getAndroidRunConfigurations(runManagerEx)

            val globallyDynamicBuildPreparationProvider = getGloballyDynamicBuildPreparationProvider()
            androidRunConfigurations.forEach { androidRunConfiguration ->
                val tasks = runManagerEx.getBeforeRunTasks(androidRunConfiguration)
                val globallyDynamicBuildPreparationTasks = tasks.filterIsInstance<GloballyDynamicBuildPreparationTask>()
                if (globallyDynamicBuildPreparationTasks.isEmpty()) {
                    val task = globallyDynamicBuildPreparationProvider.createTask(androidRunConfiguration)
                    tasks.add(0, task)
                    runManagerEx.setBeforeRunTasks(androidRunConfiguration, tasks)
                } else {
                    val otherTasks = tasks.filter { !globallyDynamicBuildPreparationTasks.contains(it) }
                    val sortedTasks = listOf(globallyDynamicBuildPreparationTasks.first()) + otherTasks
                    runManagerEx.setBeforeRunTasks(androidRunConfiguration, sortedTasks)
                }
            }
        }
    }
}