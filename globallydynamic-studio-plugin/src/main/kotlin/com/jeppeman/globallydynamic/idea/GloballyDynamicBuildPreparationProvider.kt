package com.jeppeman.globallydynamic.idea

import com.android.annotations.VisibleForTesting
import com.android.tools.idea.run.AndroidRunConfigurationBase
import com.intellij.execution.BeforeRunTask
import com.intellij.execution.BeforeRunTaskProvider
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.util.Key
import icons.PluginIcons
import javax.swing.Icon

class GloballyDynamicBuildPreparationProvider : BeforeRunTaskProvider<GloballyDynamicBuildPreparationTask>() {
    @VisibleForTesting
    fun getGloballyDynamicServerManager(runConfiguration: RunConfiguration): GloballyDynamicServerManager =
        GloballyDynamicServerManager.getInstance(runConfiguration.project)

    @VisibleForTesting
    fun shouldCreateTask(runConfiguration: RunConfiguration): Boolean = runConfiguration is AndroidRunConfigurationBase

    override fun getName(): String = "GloballyDynamic build preparation"

    override fun getId(): Key<GloballyDynamicBuildPreparationTask> = ID

    override fun getIcon(): Icon? = PluginIcons.logo1616

    override fun createTask(configuration: RunConfiguration): GloballyDynamicBuildPreparationTask? {
        return if (shouldCreateTask(configuration)) {
            GloballyDynamicBuildPreparationTask().apply { isEnabled = true }
        } else {
            null
        }
    }

    override fun executeTask(
        dataContext: DataContext,
        runConfiguration: RunConfiguration,
        executionEnvironment: ExecutionEnvironment,
        task: GloballyDynamicBuildPreparationTask
    ): Boolean {
        getGloballyDynamicServerManager(runConfiguration).start()
        return true
    }

    companion object {
        val ID = Key.create<GloballyDynamicBuildPreparationTask>("GloballyDynamic.BuildPreparationTask")
    }
}

class GloballyDynamicBuildPreparationTask :
    BeforeRunTask<GloballyDynamicBuildPreparationTask>(GloballyDynamicBuildPreparationProvider.ID)