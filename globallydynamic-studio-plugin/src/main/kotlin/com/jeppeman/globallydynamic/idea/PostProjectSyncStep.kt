package com.jeppeman.globallydynamic.idea

import com.android.tools.idea.gradle.project.sync.setup.post.ProjectSetupStep
import com.android.tools.idea.run.AndroidRunConfigurationBase
import com.intellij.execution.RunManagerEx
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project
import com.jeppeman.globallydynamic.idea.tooling.globallyDynamicGradle
import com.jeppeman.globallydynamic.idea.tooling.hasGloballyDynamicEnabled
import com.jeppeman.globallydynamic.idea.utils.runInBackground
import com.jeppeman.globallydynamic.idea.utils.startGloballyDynamicServerIfNeeded

class PostProjectSyncStep : ProjectSetupStep() {
    private fun getRunManagerX(project: Project): RunManagerEx = RunManagerEx.getInstanceEx(project)

    private fun getAndroidRunConfigurations(
        runManagerEx: RunManagerEx
    ): List<RunConfiguration> = runManagerEx.allConfigurationsList.filterIsInstance<AndroidRunConfigurationBase>()

    override fun setUpProject(project: Project) {
        project.runInBackground("Preparing GloballyDynamic") {
            project.globallyDynamicGradle.refresh()
            project.startGloballyDynamicServerIfNeeded()

            if (project.hasGloballyDynamicEnabled) {
                val runManagerEx = getRunManagerX(project)
                val androidRunConfigurations = getAndroidRunConfigurations(runManagerEx)

                val globallyDynamicBuildPreparationProvider = GloballyDynamicBuildPreparationProvider()
                androidRunConfigurations.forEach { androidRunConfiguration ->
                    val tasks = runManagerEx.getBeforeRunTasks(androidRunConfiguration)
                    val globallyDynamicBuildPreparationTasks =
                        tasks.filterIsInstance<GloballyDynamicBuildPreparationTask>()
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