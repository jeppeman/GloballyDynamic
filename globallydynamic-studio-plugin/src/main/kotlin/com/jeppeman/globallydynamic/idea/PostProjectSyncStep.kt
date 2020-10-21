package com.jeppeman.globallydynamic.idea

import com.android.annotations.VisibleForTesting
import com.android.tools.idea.gradle.project.sync.setup.post.ProjectSetupStep
import com.android.tools.idea.run.AndroidRunConfigurationBase
import com.intellij.execution.RunManagerEx
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.jeppeman.globallydynamic.idea.extensions.hasGloballyDynamicEnabled

class PostProjectSyncStep : ProjectSetupStep() {
    @VisibleForTesting
    fun getServerManager(project: Project): GloballyDynamicServerManager =
        GloballyDynamicServerManager.getInstance(project)

    @VisibleForTesting
    fun shouldInitializeServer(project: Project): Boolean =
        getModuleManager(project).modules.any(Module::hasGloballyDynamicEnabled)

    @VisibleForTesting
    fun getModuleManager(project: Project): ModuleManager = ModuleManager.getInstance(project)

    @VisibleForTesting
    fun getRunManagerX(project: Project): RunManagerEx = RunManagerEx.getInstanceEx(project)

    @VisibleForTesting
    fun getAndroidRunConfigurations(runManagerEx: RunManagerEx): List<RunConfiguration> =
        runManagerEx.allConfigurationsList.filterIsInstance<AndroidRunConfigurationBase>()

    @VisibleForTesting
    fun getGloballyDynamicBuildPreparationProvider() = GloballyDynamicBuildPreparationProvider()

    @VisibleForTesting
    fun shouldAddBuildPreparationTask(module: Module): Boolean = module.hasGloballyDynamicEnabled

    override fun setUpProject(project: Project) {
        if (shouldInitializeServer(project)) {
            getServerManager(project).start()
        }

        getModuleManager(project).modules.forEach { module ->
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
}