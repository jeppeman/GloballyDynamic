package com.jeppeman.globallydynamic.gradle

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import com.android.build.gradle.internal.tasks.factory.dependsOn
import com.google.common.annotations.VisibleForTesting
import com.jeppeman.globallydynamic.gradle.extensions.extension
import com.jeppeman.globallydynamic.gradle.extensions.findMatchingVariant
import com.jeppeman.globallydynamic.gradle.extensions.getTaskName
import com.jeppeman.globallydynamic.gradle.extensions.register
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import java.nio.file.Paths

/**
 * Entry point to the plugin; this plugin is applicable to projects which uses [AppPlugin] (com.android.application).
 * It will wait for depending dynamic feature modules to initialize and then add tasks for each build variant.
 * The tasks that will be added are [WriteConfigurationSourceFilesTask], [DependencyVerificationTask]
 * and [UploadBundleTask] - navigate to their respective source files for more information.
 */
class GloballyDynamicPlugin : Plugin<Project> {
    private val Project.baseAppModuleExtension: BaseAppModuleExtension
        get() = extensions.findByType(BaseAppModuleExtension::class.java)
            ?: throw IllegalStateException("Could not find ${BaseAppModuleExtension::class.java} for project" +
                " ${project.name}")

    private fun AppExtension.createExtensions() {
        val extensionAwareAppExtension = this as ExtensionAware

        extensionAwareAppExtension.extensions.create(
            GloballyDynamicServersExtension.NAME,
            GloballyDynamicServersExtension::class.java
        )
    }

    /**
     * Registers a [UploadBundleTask]
     */
    @VisibleForTesting
    internal fun registerUploadBundleTask(
        project: Project,
        globallyDynamicServer: GloballyDynamicServer,
        applicationVariant: ApplicationVariant
    ) {
        val uploadBundleTaskCreation = UploadBundleTask.CreationAction(
            applicationVariant = applicationVariant,
            extension = globallyDynamicServer
        )

        val uploadApkTaskProvider = project.tasks.register(uploadBundleTaskCreation)
        val packageBundleTaskProvider = project.tasks.named(
            applicationVariant.getTaskName("package", "bundle")
        )
        val signingConfigWriterTask = project.tasks.named(
            applicationVariant.getTaskName("signingConfigWriter")
        )
        uploadApkTaskProvider.dependsOn(packageBundleTaskProvider, signingConfigWriterTask)
        if (globallyDynamicServer.resolveUploadAutomatically(project)) {
            packageBundleTaskProvider.configure { task ->
                task.finalizedBy(uploadApkTaskProvider)
            }
        }
    }

    /**
     * Registers a [DependencyVerificationTask]
     */
    @VisibleForTesting
    internal fun registerDependencyVerificationTask(
        project: Project,
        globallyDynamicServer: GloballyDynamicServer?,
        applicationVariant: ApplicationVariant
    ) {
        val dependencyVerificationTaskCreation = DependencyVerificationTask.CreationAction(
            applicationVariant = applicationVariant,
            extension = globallyDynamicServer
        )

        val dependencyVerificationTaskProvider = project.tasks.register(dependencyVerificationTaskCreation)

        val packageBundleTaskName = applicationVariant.getTaskName("package", "Bundle")
        val packageBundleTaskProvider = project.tasks.named(packageBundleTaskName)
        packageBundleTaskProvider.dependsOn(dependencyVerificationTaskProvider)
    }

    /**
     * Registers a [WriteConfigurationSourceFilesTask]
     */
    @VisibleForTesting
    internal fun registerWriteConfigurationSourceFileTask(
        project: Project,
        globallyDynamicServer: GloballyDynamicServer,
        applicationVariant: ApplicationVariant,
        dynamicFeatureProjectModels: List<DynamicFeatureProjectModel>,
        appExtension: AppExtension
    ) {
        val dynamicFeatureVariantMap = mutableMapOf<Project, ApplicationVariant>()

        val buildTypeFallbacks = appExtension.buildTypes.firstOrNull { buildType ->
            buildType.name == applicationVariant.buildType.name
        }?.matchingFallbacks ?: listOf()

        val bundleResourcesTask = project.tasks.named(applicationVariant.getTaskName("bundle", "Resources"))
        val dynamicFeatureBundleResourceTasks = dynamicFeatureProjectModels
            .flatMap { (dynamicFeatureProject, dynamicFeatureAppExtension) ->
                val matchingVariant = dynamicFeatureAppExtension.findMatchingVariant(
                    applicationVariant = applicationVariant,
                    buildTypeFallbacks = buildTypeFallbacks
                )
                    ?: throw IllegalStateException(
                        "Could not find a matching variant for ${applicationVariant.name} in project " +
                            "${dynamicFeatureProject.name}, available variants: " +
                            "${dynamicFeatureAppExtension.applicationVariants.map(ApplicationVariant::getName)}"
                    )

                dynamicFeatureVariantMap[dynamicFeatureProject] = matchingVariant

                return@flatMap listOf(
                    matchingVariant.getTaskName("bundle", "Resources")
                ).map(dynamicFeatureProject.tasks::named)
            }

        val dependencies = dynamicFeatureBundleResourceTasks + bundleResourcesTask

        val dependents = listOf(
            applicationVariant.getTaskName("package", "Bundle")
        ).map(project.tasks::named)

        val outputDir = Paths.get(
            project.buildDir.absolutePath,
            "generated",
            "source",
            "globallydynamic",
            applicationVariant.name
        ).toFile()

        val writeConfigurationSourceFileTaskCreation = WriteConfigurationSourceFilesTask.CreationAction(
            applicationVariant = applicationVariant,
            extension = globallyDynamicServer,
            dynamicFeatureVariantMap = dynamicFeatureVariantMap,
            outputDir = outputDir
        )

        val provider = project.tasks.register(writeConfigurationSourceFileTaskCreation)
        provider.dependsOn(dependencies)
        dependents.forEach { dependent -> dependent.dependsOn(provider) }
        provider.get().let { task -> applicationVariant.registerJavaGeneratingTask(task, outputDir) }
    }

    @VisibleForTesting
    internal fun registerApkProducerTasks(
        project: Project,
        applicationVariant: ApplicationVariant
    ) {
        val signedBuildUniversalApkTaskCreationAction = BuildUniversalApkTask.CreationAction(
            applicationVariant = applicationVariant,
            signed = true
        ).let { project.tasks.register(it) }

        val unsignedBuildUniversalApkTaskCreationAction = BuildUniversalApkTask.CreationAction(
            applicationVariant = applicationVariant,
            signed = false
        ).let { project.tasks.register(it) }

        val signedBuildMasterApkTaskCreationAction = BuildBaseApkTask.CreationAction(
            applicationVariant = applicationVariant,
            signed = true
        ).let { project.tasks.register(it) }

        val unsignedBuildMasterApkTaskCreationAction = BuildBaseApkTask.CreationAction(
            applicationVariant = applicationVariant,
            signed = false
        ).let { project.tasks.register(it) }

        listOf(
            signedBuildUniversalApkTaskCreationAction,
            unsignedBuildUniversalApkTaskCreationAction,
            signedBuildMasterApkTaskCreationAction,
            unsignedBuildMasterApkTaskCreationAction
        ).forEach { provider ->
            val makeApksTask = project.tasks.named(
                applicationVariant.getTaskName("makeApkFromBundleFor")
            )
            val signingConfigWriterTask = project.tasks.named(
                applicationVariant.getTaskName("signingConfigWriter")
            )
            provider.dependsOn(makeApksTask, signingConfigWriterTask)
        }
    }

    /**
     * Register all variant specific tasks
     */
    @VisibleForTesting
    internal fun registerVariantTasks(
        project: Project,
        appExtension: AppExtension,
        dynamicFeatureProjectModels: List<DynamicFeatureProjectModel>
    ) {
        val globallyDynamicServers = appExtension.extension<GloballyDynamicServersExtension>()
        globallyDynamicServers.forEach { globallyDynamicServer ->
            if (globallyDynamicServer.buildVariants.isEmpty()) {
                throw IllegalArgumentException("""
                    | No build variants applied to GloballyDynamic server with name "${globallyDynamicServer.name}".
                    | To apply a build variant:
                    | 
                    | android {
                    |     globallyDynamicServers {
                    |         ${globallyDynamicServer.name} {
                    |             ...
                    |             applyToBuildVariants '<variantName>'
                    |         }
                    |     }
                    | }
                """.trimMargin())
            }
            val nonExistingVariants = globallyDynamicServer.buildVariants
                .subtract(appExtension.applicationVariants.map(ApplicationVariant::getName))

            if (nonExistingVariants.isNotEmpty()) {
                val variants = appExtension.applicationVariants.map(ApplicationVariant::getName)
                val sentenceOpening = if (nonExistingVariants.size > 1) {
                    "None of $nonExistingVariants are are valid build variants"
                } else {
                    "${nonExistingVariants.joinToString()} is not a valid build variant"
                }
                throw IllegalArgumentException("$sentenceOpening. Valid variants: $variants")
            }
        }
        appExtension.applicationVariants.forEach { applicationVariant ->
            val globallyDynamicExtension = globallyDynamicServers.firstOrNull { globallyDynamicServer ->
                globallyDynamicServer.buildVariants.contains(applicationVariant.name)
            }

            if (globallyDynamicExtension != null) {
                registerWriteConfigurationSourceFileTask(
                    globallyDynamicServer = globallyDynamicExtension,
                    applicationVariant = applicationVariant,
                    dynamicFeatureProjectModels = dynamicFeatureProjectModels,
                    appExtension = appExtension,
                    project = project
                )

                registerUploadBundleTask(
                    project = project,
                    globallyDynamicServer = globallyDynamicExtension,
                    applicationVariant = applicationVariant
                )
            }

            registerDependencyVerificationTask(
                project = project,
                globallyDynamicServer = globallyDynamicExtension,
                applicationVariant = applicationVariant
            )

            registerApkProducerTasks(
                project = project,
                applicationVariant = applicationVariant
            )
        }
    }

    @VisibleForTesting
    internal fun afterEvaluate(project: Project) {
        val appExtension = project.baseAppModuleExtension
        val dynamicFeatures = appExtension.dynamicFeatures

        if (dynamicFeatures.isEmpty()) {
            return
        }

        val dynamicFeatureProjects = dynamicFeatures.mapNotNull(project.rootProject::findProject)
        val dynamicFeatureProjectModels = dynamicFeatureProjects.map { dynamicFeatureProject ->
            val dynamicFeatureExtension = dynamicFeatureProject.extensions.findByType(AppExtension::class.java)
            if (dynamicFeatureExtension?.applicationVariants == null
                || dynamicFeatureExtension.applicationVariants.isEmpty()) {
                throw IllegalStateException("No application variants found for dynamic feature module " +
                    dynamicFeatureProject.name)
            }

            DynamicFeatureProjectModel(
                project = dynamicFeatureProject,
                appExtension = dynamicFeatureProject.extensions.findByType(AppExtension::class.java)!!
            )
        }

        registerVariantTasks(
            project = project,
            appExtension = appExtension,
            dynamicFeatureProjectModels = dynamicFeatureProjectModels
        )
    }

    override fun apply(project: Project) {
        if (!project.plugins.hasPlugin("com.android.application")) {
            throw IllegalStateException("Missing plugin com.android.application")
        }

        project.baseAppModuleExtension.createExtensions()

        project.rootProject.gradle.projectsEvaluated { afterEvaluate(project) }
    }
}

internal data class DynamicFeatureProjectModel(
    val project: Project,
    val appExtension: AppExtension
)