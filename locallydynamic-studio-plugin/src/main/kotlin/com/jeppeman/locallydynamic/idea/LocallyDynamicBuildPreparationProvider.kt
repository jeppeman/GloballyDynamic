package com.jeppeman.locallydynamic.idea

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

class LocallyDynamicBuildPreparationProvider : BeforeRunTaskProvider<LocallyDynamicBuildPreparationTask>() {
    @VisibleForTesting
    fun getLocallyDynamicServerManager(runConfiguration: RunConfiguration): LocallyDynamicServerManager =
        LocallyDynamicServerManager.getInstance(runConfiguration.project)

    @VisibleForTesting
    fun shouldCreateTask(runConfiguration: RunConfiguration): Boolean = runConfiguration is AndroidRunConfigurationBase

    override fun getName(): String = "LocallyDynamic build preparation"

    override fun getId(): Key<LocallyDynamicBuildPreparationTask> = ID

    override fun getIcon(): Icon? = PluginIcons.logo1616

    override fun createTask(configuration: RunConfiguration): LocallyDynamicBuildPreparationTask? {
        return if (shouldCreateTask(configuration)) {
            LocallyDynamicBuildPreparationTask().apply { isEnabled = true }
        } else {
            null
        }
    }

    override fun executeTask(
        dataContext: DataContext,
        runConfiguration: RunConfiguration,
        executionEnvironment: ExecutionEnvironment,
        task: LocallyDynamicBuildPreparationTask
    ): Boolean {
        getLocallyDynamicServerManager(runConfiguration).start()
        return true
    }

    companion object {
        val ID = Key.create<LocallyDynamicBuildPreparationTask>("LocallyDynamic.BuildPreparationTask")
    }
}

class LocallyDynamicBuildPreparationTask :
    BeforeRunTask<LocallyDynamicBuildPreparationTask>(LocallyDynamicBuildPreparationProvider.ID)