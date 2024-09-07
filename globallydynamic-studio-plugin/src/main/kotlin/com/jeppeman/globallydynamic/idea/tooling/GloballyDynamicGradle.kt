package com.jeppeman.globallydynamic.idea.tooling

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import org.gradle.tooling.BuildAction
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ProjectConnection
import org.gradle.tooling.model.GradleProject
import org.jetbrains.plugins.gradle.util.GradleConstants.INIT_SCRIPT_CMD_OPTION
import java.io.File
import java.util.*

class GloballyDynamicGradle(
    private val ideaProject: Project
) {
    private var projectConnection: ProjectConnection? = null
    private var gradleProject: GradleProject? = null
    private var projectInfo: GloballyDynamicProjectInfo? = null
    val gradleProjects: Set<GradleProject> get() = gradleProjectOrInit()?.allProjects ?: emptySet()

    val GradleProject.hasGloballyDynamicEnabled: Boolean
        get() = projectInfoOrInit()
            ?.projects
            ?.find { it.path == path }
            ?.globallyDynamicEnabled == true

    private fun init() {
        projectConnection = ideaProject.gradleConnection()
        gradleProject = projectConnection?.getModel(GradleProject::class.java)
        projectInfo = fetchProjectInfo()
    }

    fun refresh() {
        projectConnection?.close()
        init()
    }

    private fun connectionOrInit(): ProjectConnection? {
        if (projectConnection != null) return projectConnection
        init()
        return projectConnection
    }

    private fun gradleProjectOrInit(): GradleProject? {
        if (gradleProject != null) return gradleProject
        init()
        return gradleProject
    }

    private fun projectInfoOrInit(): GloballyDynamicProjectInfo? {
        if (projectInfo != null) return projectInfo
        init()
        return projectInfo
    }

    private fun <T> runAction(buildAction: BuildAction<T>): T? {
        return connectionOrInit()?.action(buildAction)
            ?.withArguments(initScriptCmdLineArgs())
            ?.run()
    }

    private fun fetchProjectInfo(): GloballyDynamicProjectInfo? {
        return runAction(GloballyDynamicProjectInfoBuildAction())
    }

    fun destroy() {
        projectConnection?.close()
        unregister(ideaProject)
        projectConnection = null
        gradleProject = null
        projectInfo = null
    }

    companion object {
        private val instancesForProjects = mutableMapOf<Project, GloballyDynamicGradle>()

        fun unregister(project: Project) {
            instancesForProjects.remove(project)
        }

        fun getInstance(project: Project): GloballyDynamicGradle {
            if (project in instancesForProjects) {
                return instancesForProjects[project]!!
            }

            instancesForProjects[project] = GloballyDynamicGradle(project)

            return instancesForProjects[project]!!
        }
    }
}

val Project.hasGloballyDynamicEnabled: Boolean
    get() = globallyDynamicGradle.run { gradleProjects.any { it.hasGloballyDynamicEnabled } }

val Project.globallyDynamicGradle: GloballyDynamicGradle get() = GloballyDynamicGradle.getInstance(this)

val GradleProject.allProjects: Set<GradleProject>
    get() = children + children.flatMap { it.allProjects } + this

private fun Project.gradleConnection(): ProjectConnection = GradleConnector.newConnector()
    .forProjectDirectory(File(basePath!!))
    .connect()

private fun initScriptCmdLineArgs(): List<String> {
    val initScriptContents = """
            initscript {
                dependencies {
                    ${createClassPathString(jarPaths)}
                }
                
            }
            allprojects {
                apply plugin: ${GloballyDynamicToolingPlugin::class.java.name}
            }
        """.trimIndent()
    val scriptFile = FileUtil.createTempFile("studio.globallydynamic.tooling", ".gradle")
    scriptFile.deleteOnExit()
    scriptFile.writeText(initScriptContents)
    return mutableListOf(INIT_SCRIPT_CMD_OPTION, scriptFile.absolutePath)
}

private fun createClassPathString(paths: List<String?>): String? {
    val classpath = StringBuilder()
    classpath.append("classpath files([")
    val pathCount = paths.size
    for (i in 0 until pathCount) {
        val jarPath = escapeGroovyStringLiteral(paths[i]!!)
        classpath.append("'").append(jarPath).append("'")
        if (i < pathCount - 1) {
            classpath.append(", ")
        }
    }
    classpath.append("])")
    return classpath.toString()
}

private fun escapeGroovyStringLiteral(s: String): String {
    val sb = java.lang.StringBuilder(s.length + 5)
    var i = 0
    val n = s.length
    while (i < n) {
        val c = s[i]
        if (c == '\\' || c == '\'') {
            sb.append('\\')
        }
        sb.append(c)
        i++
    }
    return sb.toString()
}

private val jarPaths: List<String>
    get() = listOf(
        getJarPathForClass(GloballyDynamicToolingPlugin::class.java),
        getJarPathForClass(GloballyDynamicProjectInfo::class.java),
        getJarPathForClass(GloballyDynamicProjectInfoImpl::class.java),
        getJarPathForClass(GloballyDynamicGradleProject::class.java),
        getJarPathForClass(GloballyDynamicGradleProjectImpl::class.java),
        getJarPathForClass(GloballyDynamicProjectInfoModelBuilder::class.java),
    ).filter(Objects::nonNull)

private fun getJarPathForClass(aClass: Class<*>): String {
    return FileUtil.toCanonicalPath(PathManager.getJarPathForClass(aClass))
}