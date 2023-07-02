package com.jeppeman.globallydynamic.idea.tooling

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import javax.inject.Inject

class GloballyDynamicToolingPlugin @Inject constructor(
    private val toolingModelBuilderRegistry: ToolingModelBuilderRegistry
) : Plugin<Project> {
    override fun apply(project: Project) {
        toolingModelBuilderRegistry.register(GloballyDynamicProjectInfoModelBuilder())
    }
}
