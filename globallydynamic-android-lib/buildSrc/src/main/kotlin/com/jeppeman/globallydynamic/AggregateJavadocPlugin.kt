package com.jeppeman.globallydynamic

import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.JavadocMemberLevel
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.task
import java.io.File

class AggregateJavadocPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val rootProject = target.rootProject
        rootProject.gradle.projectsEvaluated {
            val javadocTasks = rootProject.subprojects.filter { subProject ->
                subProject.extra.has("javadocTask")
            }.associate { subProject ->
                subProject.extensions.getByType(LibraryExtension::class.java) to subProject.tasks.getByName(subProject.extra["javadocTask"]?.toString()!!) as Javadoc
            }

            rootProject.task("aggregateJavadoc", Javadoc::class) {
                description = "Aggregates Javadoc from sub projects"
                group = JavaBasePlugin.DOCUMENTATION_GROUP
                source = javadocTasks.values.map { it.source }.reduce { acc, fileTree -> acc + fileTree }

                dependsOn(*javadocTasks.values.toTypedArray())
                options.memberLevel = JavadocMemberLevel.PROTECTED
                setDestinationDir(File("${project.rootProject.buildDir}", "docs"))
                exclude("**/R.java", "**/BuildConfig.java")
            }

            javadocTasks.values.forEach { task ->
                task.doLast {
                    rootProject.tasks.getByName("aggregateJavadoc", Javadoc::class) {
                        classpath += task.classpath
                    }
                }
            }
        }
    }
}