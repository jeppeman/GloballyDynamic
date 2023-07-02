package com.jeppeman.globallydynamic.idea.tooling

import org.gradle.api.Project
import org.gradle.tooling.BuildAction
import org.gradle.tooling.BuildController
import org.gradle.tooling.provider.model.ToolingModelBuilder
import java.io.Serializable

class GloballyDynamicProjectInfoModelBuilder : ToolingModelBuilder {
    override fun canBuild(modelName: String): Boolean {
        return modelName == GloballyDynamicProjectInfo::class.java.name
    }

    override fun buildAll(modelName: String, project: Project): Any {
        return GloballyDynamicProjectInfoImpl(
            projects = project.allprojects.map { p ->
                GloballyDynamicGradleProjectImpl(
                    path = p.path,
                    plugins = p.plugins.map { it.javaClass.name }.toSet()
                )
            }.toSet()
        )
    }
}

interface GloballyDynamicProjectInfo {
    val projects: Set<GloballyDynamicGradleProject>
}

class GloballyDynamicProjectInfoImpl(
    override val projects: Set<GloballyDynamicGradleProject>
) : GloballyDynamicProjectInfo, Serializable

interface GloballyDynamicGradleProject {
    val path: String
    val plugins: Set<String>
    val globallyDynamicEnabled: Boolean
}

data class GloballyDynamicGradleProjectImpl(
   override val path: String,
   override val plugins: Set<String>,
   override val globallyDynamicEnabled: Boolean = GLOBALLY_DYNAMIC_GRADLE_PLUGIN in plugins,
) : GloballyDynamicGradleProject, Serializable

class GloballyDynamicProjectInfoBuildAction : BuildAction<GloballyDynamicProjectInfo?>, Serializable {
    override fun execute(bc: BuildController?): GloballyDynamicProjectInfo? {
        return bc?.findModel(GloballyDynamicProjectInfo::class.java)
    }
}

private const val GLOBALLY_DYNAMIC_GRADLE_PLUGIN = "com.jeppeman.globallydynamic.gradle.GloballyDynamicPlugin"
