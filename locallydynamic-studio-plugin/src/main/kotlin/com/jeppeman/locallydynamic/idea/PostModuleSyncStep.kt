package com.jeppeman.locallydynamic.idea

import com.android.annotations.VisibleForTesting
import com.android.tools.idea.gradle.project.sync.setup.post.ModuleSetupStep
import com.android.tools.idea.run.AndroidRunConfigurationBase
import com.intellij.execution.RunManagerEx
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.jeppeman.locallydynamic.idea.extensions.hasLocallyDynamicEnabled

class PostModuleSyncStep : ModuleSetupStep() {
    @VisibleForTesting
    fun getRunManagerX(project: Project): RunManagerEx = RunManagerEx.getInstanceEx(project)

    @VisibleForTesting
    fun getAndroidRunConfigurations(runManagerEx: RunManagerEx): List<RunConfiguration> =
        runManagerEx.allConfigurationsList.filterIsInstance<AndroidRunConfigurationBase>()
    @VisibleForTesting
    fun getLocallyDynamicBuildPreparationProvider() = LocallyDynamicBuildPreparationProvider()

    @VisibleForTesting
    fun shouldAddBuildPreparationTask(module: Module): Boolean = module.hasLocallyDynamicEnabled

    override fun setUpModule(module: Module, progressIndicator: ProgressIndicator?) {
        if (shouldAddBuildPreparationTask(module)) {
            val runManagerEx = getRunManagerX(module.project)
            val androidRunConfigurations = getAndroidRunConfigurations(runManagerEx)

            val locallyDynamicBuildPreparationProvider = getLocallyDynamicBuildPreparationProvider()
            androidRunConfigurations.forEach { androidRunConfiguration ->
                val tasks = runManagerEx.getBeforeRunTasks(androidRunConfiguration)
                val locallyDynamicBuildPreparationTasks = tasks.filterIsInstance<LocallyDynamicBuildPreparationTask>()
                if (locallyDynamicBuildPreparationTasks.isEmpty()) {
                    val task = locallyDynamicBuildPreparationProvider.createTask(androidRunConfiguration)
                    tasks.add(0, task)
                    runManagerEx.setBeforeRunTasks(androidRunConfiguration, tasks)
                } else {
                    val otherTasks = tasks.filter { !locallyDynamicBuildPreparationTasks.contains(it) }
                    val sortedTasks = listOf(locallyDynamicBuildPreparationTasks.first()) + otherTasks
                    runManagerEx.setBeforeRunTasks(androidRunConfiguration, sortedTasks)
                }
            }
        }
    }
}