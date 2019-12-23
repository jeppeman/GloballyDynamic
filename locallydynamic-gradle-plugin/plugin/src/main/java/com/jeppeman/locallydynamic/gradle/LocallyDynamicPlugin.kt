package com.jeppeman.locallydynamic.gradle

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import com.android.build.gradle.internal.tasks.factory.dependsOn
import com.google.common.annotations.VisibleForTesting
import com.jeppeman.locallydynamic.gradle.extensions.findMatchingVariant
import com.jeppeman.locallydynamic.gradle.extensions.getTaskName
import com.jeppeman.locallydynamic.gradle.extensions.register
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
class LocallyDynamicPlugin : Plugin<Project> {
    private val Project.baseAppModuleExtension: BaseAppModuleExtension
        get() = extensions.findByType(BaseAppModuleExtension::class.java)
            ?: throw IllegalStateException("Could not find ${BaseAppModuleExtension::class.java} for project" +
                " ${project.name}")

    /**
     * Extend the android buildType configuration with a locallyDynamic { } object
     */
    private fun createLocallyDynamicExtension(appExtension: AppExtension) {
        appExtension.buildTypes?.configureEach { buildType ->
            val extensionAwareBuildType = buildType as? ExtensionAware
            extensionAwareBuildType?.extensions?.create(
                LocallyDynamicExtension.NAME,
                LocallyDynamicExtension::class.java,
                false // enabled
            )
        }
    }

    /**
     * Registers a [UploadBundleTask]
     */
    @VisibleForTesting
    internal fun registerUploadBundleTask(
        project: Project,
        locallyDynamicExtension: LocallyDynamicExtension,
        applicationVariant: ApplicationVariant
    ) {
        val uploadBundleTaskCreation = UploadBundleTask.CreationAction(
            applicationVariant = applicationVariant,
            extension = locallyDynamicExtension
        )

        val uploadApkTaskProvider = project.tasks.register(uploadBundleTaskCreation)
        val packageBundleTaskProvider = project.tasks.named(
            applicationVariant.getTaskName("package", "bundle")
        )
        val signingConfigWriterTask = project.tasks.named(
            applicationVariant.getTaskName("signingConfigWriter")
        )
        uploadApkTaskProvider.dependsOn(packageBundleTaskProvider, signingConfigWriterTask)
        packageBundleTaskProvider.configure { task ->
            task.finalizedBy(uploadApkTaskProvider)
        }
    }

    /**
     * Registers a [DependencyVerificationTask]
     */
    @VisibleForTesting
    internal fun registerDependencyVerificationTask(
        project: Project,
        locallyDynamicExtension: LocallyDynamicExtension?,
        applicationVariant: ApplicationVariant
    ) {
        val dependencyVerificationTaskCreation = DependencyVerificationTask.CreationAction(
            applicationVariant = applicationVariant,
            extension = locallyDynamicExtension
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
        locallyDynamicExtension: LocallyDynamicExtension,
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
            "locallydynamic",
            applicationVariant.name
        ).toFile()

        val writeConfigurationSourceFileTaskCreation = WriteConfigurationSourceFilesTask.CreationAction(
            applicationVariant = applicationVariant,
            extension = locallyDynamicExtension,
            dynamicFeatureVariantMap = dynamicFeatureVariantMap,
            outputDir = outputDir
        )

        val provider = project.tasks.register(writeConfigurationSourceFileTaskCreation)
        provider.dependsOn(dependencies)
        dependents.forEach { dependent -> dependent.dependsOn(provider) }
        provider.get().let { task -> applicationVariant.registerJavaGeneratingTask(task, outputDir) }
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
        appExtension.applicationVariants.forEach { applicationVariant ->
            val buildType = appExtension.buildTypes
                ?.firstOrNull { buildType -> buildType.name == applicationVariant.buildType.name }
                as? ExtensionAware

            val locallyDynamicExtension = buildType?.extensions?.findByType(LocallyDynamicExtension::class.java)

            if (locallyDynamicExtension?.enabled == true) {
                registerWriteConfigurationSourceFileTask(
                    locallyDynamicExtension = locallyDynamicExtension,
                    applicationVariant = applicationVariant,
                    dynamicFeatureProjectModels = dynamicFeatureProjectModels,
                    appExtension = appExtension,
                    project = project
                )

                registerUploadBundleTask(
                    project = project,
                    locallyDynamicExtension = locallyDynamicExtension,
                    applicationVariant = applicationVariant
                )
            }

            registerDependencyVerificationTask(
                project = project,
                locallyDynamicExtension = locallyDynamicExtension,
                applicationVariant = applicationVariant
            )
        }
    }

    /**
     * Recursively polls the readiness of a dynamic feature project based on whether
     * its [AppExtension] has been initialized.
     */
    @VisibleForTesting
    internal fun onDynamicFeatureProjectReady(
        dynamicFeatureProject: Project,
        recursionDepth: Int = 0,
        block: (AppExtension) -> Unit
    ) {
        // TODO: find another base case; there is probably a better way to wait for a project
        if (recursionDepth == 50) {
            throw IllegalStateException("com.android.dynamic-feature must be applied to project ${dynamicFeatureProject.name}")
        }

        val extension = dynamicFeatureProject.extensions.findByType(AppExtension::class.java)
        if (extension?.applicationVariants?.isNotEmpty() == true) {
            block(extension)
        } else {
            dynamicFeatureProject.afterEvaluate {
                val maybeExtension = dynamicFeatureProject.extensions.findByType(AppExtension::class.java)
                if (maybeExtension?.applicationVariants?.isNotEmpty() == true) {
                    block(maybeExtension)
                } else {
                    onDynamicFeatureProjectReady(dynamicFeatureProject, recursionDepth + 1, block)
                }
            }
        }
    }

    /**
     * Wait for dynamic feature projects to initialize (if any) in order to do further processing
     */
    @VisibleForTesting
    internal fun afterEvaluate(project: Project) {
        val appExtension = project.baseAppModuleExtension
        val dynamicFeatures = appExtension.dynamicFeatures

        if (dynamicFeatures.isEmpty()) {
            return
        }

        val dynamicFeatureProjects = dynamicFeatures.mapNotNull(project.rootProject::findProject)

        if (dynamicFeatureProjects.isEmpty()) {
            return
        }

        val dynamicFeatureProjectModels = mutableListOf<DynamicFeatureProjectModel>()
        dynamicFeatureProjects.forEach { dynamicFeatureProject ->
            onDynamicFeatureProjectReady(dynamicFeatureProject) { dynamicFeatureAppExtension ->
                dynamicFeatureProjectModels.add(DynamicFeatureProjectModel(
                    project = dynamicFeatureProject,
                    appExtension = dynamicFeatureAppExtension
                ))

                // Are all depending dynamic feature projects ready?
                if (dynamicFeatureProjectModels.size == dynamicFeatureProjects.size) {
                    registerVariantTasks(
                        project = project,
                        appExtension = appExtension,
                        dynamicFeatureProjectModels = dynamicFeatureProjectModels
                    )
                }
            }
        }
    }

    override fun apply(project: Project) {
        if (!project.plugins.hasPlugin("com.android.application")) {
            throw IllegalStateException("Missing plugin com.android.application")
        }

        createLocallyDynamicExtension(project.baseAppModuleExtension)

        project.afterEvaluate(::afterEvaluate)
    }
}

internal data class DynamicFeatureProjectModel(
    val project: Project,
    val appExtension: AppExtension
)