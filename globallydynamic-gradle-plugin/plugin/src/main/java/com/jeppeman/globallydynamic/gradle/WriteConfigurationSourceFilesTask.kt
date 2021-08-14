package com.jeppeman.globallydynamic.gradle

import com.android.SdkConstants
import com.android.aapt.Resources
import com.android.build.gradle.api.ApplicationVariant
import com.android.builder.packaging.JarMerger
import com.android.tools.build.bundletool.model.AndroidManifest
import com.android.tools.build.bundletool.model.ManifestDeliveryElement
import com.jeppeman.globallydynamic.gradle.extensions.deleteCompletely
import com.jeppeman.globallydynamic.gradle.extensions.getTaskName
import com.jeppeman.globallydynamic.gradle.extensions.stackTraceToString
import com.jeppeman.globallydynamic.gradle.extensions.unzip
import com.jeppeman.globallydynamic.gradle.generators.*
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*
import java.io.File
import java.io.Serializable
import java.nio.file.Files

/**
 * A task that generates configuration source files which are necessary to interact with the globally dynamic server.
 * Used as follows: ./gradlew writeGloballyDynamicConfigurationSourceFilesFor<Flavor><BuildType> e.g
 * ./gradlew writeGloballyDynamicConfigurationSourceFilesForFreeDebug
 *
 * There is no need to call this task manually since it hooks in to the compilation process.
 */
open class WriteConfigurationSourceFilesTask : DefaultTask() {
    @get:Input
    internal lateinit var serverInfo: GloballyDynamicServerInfoDto
        private set

    @get:Input
    var throttleDownloadBy: Long = 0
        private set

    @get:Nested
    lateinit var dynamicFeatureInputs: List<DynamicFeatureInput>
        private set

    @get:Input
    lateinit var applicationId: String
        private set

    @get:Input
    var version: Int = -1
        private set

    @get:InputFiles
    lateinit var linkedBundleRes: FileCollection
        private set
    // Used to make sure cache is invalidated when LINKED_RES_FOR_BUNDLE file for any
    // dynamic feature project changes
    // TODO: find a better way to to this
    @get:Input
    @Suppress("unused")
    val linkedBundleResMtimes: Long
        get() = dynamicFeatureInputs.flatMap(DynamicFeatureInput::linkedBundleRes)
            .filter(File::exists)
            .fold(0L) { acc, curr -> acc + Files.getLastModifiedTime(curr.toPath()).toMillis() }

    @get:OutputDirectory
    lateinit var outputDir: File
        private set

    @get:Input
    lateinit var variantName: String
        private set

    @TaskAction
    fun doTaskAction() {
        val dynamicFeatureManifests = dynamicFeatureInputs.associate { input ->
            input.name to input.linkedBundleRes
                .first(File::exists)
                .extractAndroidManifest()
                ?.manifestDeliveryElement
                ?.orElse(null)
        }.filter { entry -> entry.value != null }
            .mapValues { entry -> entry.value as ManifestDeliveryElement }

        val installTimeDependentDynamicFeatures = dynamicFeatureManifests.filter { (_, manifestDeliveryElement) ->
            manifestDeliveryElement.hasInstallTimeElement()
        }
        val onDemandDependentDynamicFeatures = dynamicFeatureManifests.filter { (_, manifestDeliveryElement) ->
            manifestDeliveryElement.hasOnDemandElement()
        }

        val mainActivityName = linkedBundleRes.first(File::exists)
            .extractAndroidManifest()
            ?.extractMainActivityName()

        val fileGenerators = listOf(
            ModuleConditionsGenerator(outputDir),
            DeviceFeatureConditionGenerator(outputDir),
            UserCountriesConditionGenerator(outputDir),
            GloballyDynamicBuildConfigGenerator(
                outputFile = outputDir,
                installTimeFeatures = installTimeDependentDynamicFeatures,
                onDemandFeatures = onDemandDependentDynamicFeatures,
                serverUrl = serverInfo.serverUrl,
                applicationId = applicationId,
                mainActivityFullyQualifiedName = mainActivityName,
                version = version,
                variantName = variantName,
                throttleDownloadBy = throttleDownloadBy
            )
        )

        fileGenerators.forEach(Generator::generate)
    }

    class DynamicFeatureInput(
        @get:Input
        val name: String,
        @get:InputFiles
        @get:PathSensitive(PathSensitivity.ABSOLUTE)
        val linkedBundleRes: FileCollection
    ) : Serializable

    class CreationAction(
        applicationVariant: ApplicationVariant,
        private val outputDir: File,
        private val extension: GloballyDynamicServer,
        private val dynamicFeatureVariantMap: Map<Project, ApplicationVariant>
    ) : VariantTaskAction<WriteConfigurationSourceFilesTask>(applicationVariant) {
        override val name: String
            get() =
                applicationVariant.getTaskName("writeGloballyDynamicConfigurationSourceFilesFor")

        override fun execute(task: WriteConfigurationSourceFilesTask) {
            task.variantName = applicationVariant.name
            task.applicationId = applicationVariant.applicationId!!
            task.version = applicationVariant.versionCode
            task.outputDir = outputDir
            // AGP 3.5 and 3.6 compatibility
            task.linkedBundleRes = task.project.files(
                task.project
                    .getLinkedBundleResDir(applicationVariant)
                    .resolve("bundled-res.ap_")
                    .toFile(),
                task.project
                    .getLinkedBundleResDir(applicationVariant)
                    .resolve(applicationVariant.getTaskName("bundle", "Resources"))
                    .resolve("bundled-res.ap_")
                    .toFile()
            )

            task.dynamicFeatureInputs = dynamicFeatureVariantMap.map { (project, variant) ->
                DynamicFeatureInput(
                    name = project.name,
                    linkedBundleRes = project.files(
                        project.getLinkedBundleResDir(variant)
                            .resolve("bundled-res.ap_")
                            .toFile(),
                        project.getLinkedBundleResDir(variant)
                            .resolve(variant.getTaskName("bundle", "Resources"))
                            .resolve("bundled-res.ap_")
                            .toFile()
                    )
                )
            }
            task.throttleDownloadBy = extension.resolveThrottleDownloadBy(task.project)
            task.serverInfo = task.project.resolveServerInfo(extension)
        }
    }
}

private fun Project.getLinkedBundleResDir(applicationVariant: ApplicationVariant) =
    buildDir.toPath()
        .resolve("intermediates")
        .resolve("linked_res_for_bundle")
        .resolve(applicationVariant.name)

private const val ANDROID_XML_NS = "http://schemas.android.com/apk/res/android"

private fun AndroidManifest.extractMainActivityName(): String? =
    try {
        manifestRoot
            .element
            .getChildElement("application")
            .getChildrenElements("activity")
            .filter { element ->
                if (element.name == "activity") {
                    val actionMainExists = element.getChildrenElements("intent-filter")
                        ?.findFirst()
                        ?.takeIf { it.isPresent }
                        ?.get()
                        ?.getChildrenElements("action")
                        ?.anyMatch { action ->
                            action.getAttribute(ANDROID_XML_NS, "name")
                                ?.takeIf { it.isPresent }
                                ?.get()
                                ?.valueAsString == "android.intent.action.MAIN"
                        }

                    return@filter actionMainExists == true
                }

                return@filter false
            }
            .findFirst()
            .takeIf { it.isPresent }
            ?.get()
            ?.getAttribute(ANDROID_XML_NS, "name")
            ?.takeIf { it.isPresent }
            ?.get()
            ?.valueAsString
    } catch (exception: Exception) {
        System.err.println("Failed to extract main activity name: ${exception.stackTraceToString()}")
        null
    }

private fun File.extractAndroidManifest(): AndroidManifest? = try {
    val linkedBundleResPath = toPath()
    val outputDir = linkedBundleResPath.parent.toFile()
    val outputZip = File(
        outputDir,
        "bundle_res.zip"
    )

    JarMerger(outputZip.toPath()).use { jarMerger ->
        jarMerger.setCompressionLevel(0)

        jarMerger.addJar(linkedBundleResPath)
    }

    outputZip.unzip(outputDir.absolutePath)

    outputZip.toPath().deleteCompletely()

    val manifestProtoFile = File(outputDir, SdkConstants.FN_ANDROID_MANIFEST_XML)

    val manifestProtoRootNode = manifestProtoFile.inputStream().use {
        Resources.XmlNode
            .parseFrom(it)
    }

    AndroidManifest.create(manifestProtoRootNode)
} catch (exception: Exception) {
    System.err.println("Failed to extract AndroidManifest: ${exception.stackTraceToString()}")
    null
}