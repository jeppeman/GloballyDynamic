package com.jeppeman.globallydynamic.gradle

import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import com.jeppeman.globallydynamic.gradle.extensions.findMatchingVariant
import com.jeppeman.globallydynamic.gradle.extensions.getTaskName
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

private const val GLOBALLY_DYNAMIC_SELF_HOSTED_MAVEN_COORDINATES = "com.jeppeman.globallydynamic.android:selfhosted"
private const val GLOBALLY_DYNAMIC_ALL_MAVEN_COORDINATES = "com.jeppeman.globallydynamic.android:all"

/**
 * A task that verifies that the dependency setup is valid to run GloballyDynamic.
 * Used as follows: ./gradlew verifyGloballyDynamicDependenciesFor<Flavor><BuildType> e.g
 * ./gradlew verifyGloballyDynamicDependenciesForDebug
 *
 * There is no need to call this task manually since it hooks in to the compilation process.
 */
open class DependencyVerificationTask : DefaultTask() {
    @get:Input
    @get:Optional
    var extension: GloballyDynamicServer? = null
        private set

    @get:Input
    lateinit var buildType: String
        private set

    @get:Input
    lateinit var variantName: String
        private set

    private fun MutableList<String>.collectDependenciesForApplicationVariant(
        dependencySet: DependencySet,
        applicationVariant: ApplicationVariant,
        buildTypeFallbacks: List<String>,
        rootProject: Project
    ) {
        dependencySet.forEach { dep ->
            if (dep.group?.startsWith(rootProject.name) == true) {
                val pathPrefix = dep.group!!.split(rootProject.name)[1]
                val projectPath = if (pathPrefix.isNotBlank()) {
                    "${pathPrefix.replace('.', ':')}:${dep.name}"
                } else {
                    ":${dep.name}"
                }
                if (!contains(projectPath)) {
                    add(projectPath)
                    val project = rootProject.findProject(projectPath)
                    when (val baseExtension = project?.extensions?.findByType(BaseExtension::class.java)) {
                        is LibraryExtension -> {
                            baseExtension.findMatchingVariant(applicationVariant, buildTypeFallbacks)?.let { matchingVariant ->
                                collectDependenciesForApplicationVariant(
                                    matchingVariant.runtimeConfiguration.allDependencies,
                                    applicationVariant,
                                    buildTypeFallbacks,
                                    rootProject
                                )
                            }
                        }
                        is AppExtension -> {
                            baseExtension.findMatchingVariant(applicationVariant, buildTypeFallbacks)?.let { matchingVariant ->
                                collectDependenciesForApplicationVariant(
                                    matchingVariant.runtimeConfiguration.allDependencies,
                                    applicationVariant,
                                    buildTypeFallbacks,
                                    rootProject
                                )
                            }
                        }
                    }
                }
            } else {
                val coordinates = "${dep.group}:${dep.name}"
                if (!contains(coordinates)) {
                    add(coordinates)
                }
            }
        }
    }

    @TaskAction
    fun doTaskAction() {
        if (extension?.disableDependencyCheck == true) {
            return
        }

        val appExtension = project.extensions.findByType(BaseAppModuleExtension::class.java) ?: return
        val applicationVariant = appExtension.applicationVariants.find { it.name == variantName } ?: return

        val buildTypeFallbacks = appExtension.buildTypes.firstOrNull { buildType ->
            buildType.name == applicationVariant.buildType.name
        }?.matchingFallbacks ?: listOf()

        val dynamicFeatureDependencies = appExtension.dynamicFeatures.mapNotNull { path ->
            project.rootProject.findProject(path)
                ?.extensions
                ?.findByType(AppExtension::class.java)
                ?.findMatchingVariant(applicationVariant, buildTypeFallbacks)
                ?.runtimeConfiguration
                ?.allDependencies
        }

        val collectedDependencies = mutableListOf<String>()
        collectedDependencies.collectDependenciesForApplicationVariant(
            applicationVariant.runtimeConfiguration.allDependencies,
            applicationVariant,
            buildTypeFallbacks,
            project.rootProject
        )
        dynamicFeatureDependencies.forEach { dynamicFeatureDependencySet ->
            collectedDependencies.collectDependenciesForApplicationVariant(
                dynamicFeatureDependencySet,
                applicationVariant,
                buildTypeFallbacks,
                project.rootProject
            )
        }

        val globallyDynamicSelfHostedOrAllDependency = collectedDependencies.firstOrNull { dependency ->
            dependency == GLOBALLY_DYNAMIC_SELF_HOSTED_MAVEN_COORDINATES
                || dependency == GLOBALLY_DYNAMIC_ALL_MAVEN_COORDINATES
        }

        if (globallyDynamicSelfHostedOrAllDependency != null && extension == null) {
            project.logger.error("$globallyDynamicSelfHostedOrAllDependency is included as a dependency, but the gradle plugin has not " +
                "been properly applied for variant $variantName.\nTo enable it for the variant add the following to " +
                "your build.gradle:\n\n" +
                "android {\n" +
                "    globallyDynamicServers {\n" +
                "        myServerName {\n" +
                "            applyToBuildVariants '$variantName'\n" +
                "            ...\n" +
                "        }\n" +
                "    }\n" +
                "}")
        } else if (globallyDynamicSelfHostedOrAllDependency == null && extension != null) {
            project.logger.error("$GLOBALLY_DYNAMIC_SELF_HOSTED_MAVEN_COORDINATES or " +
                "$GLOBALLY_DYNAMIC_ALL_MAVEN_COORDINATES must be declared as a dependency in " +
                "order to use GloballyDynamic; to enabled it, add the following to your build.gradle:\n\n" +
                "dependencies {\n" +
                "    implementation '$GLOBALLY_DYNAMIC_SELF_HOSTED_MAVEN_COORDINATES:x.x'\n" +
                "    ...\n" +
                "}")
        }
    }

    class CreationAction(
        applicationVariant: ApplicationVariant,
        private val extension: GloballyDynamicServer?
    ) : VariantTaskAction<DependencyVerificationTask>(applicationVariant) {
        override val name: String
            get() = applicationVariant.getTaskName("verifyGloballyDynamicDependenciesFor")

        override fun execute(task: DependencyVerificationTask) {
            task.variantName = applicationVariant.name
            task.buildType = applicationVariant.buildType.name
            task.extension = extension
        }
    }
}