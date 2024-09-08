package com.jeppeman.globallydynamic.gradle

import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.internal.errors.DeprecationReporterImpl
import com.android.build.gradle.internal.errors.SyncIssueReporterImpl
import com.android.build.gradle.internal.lint.LintFromMaven
import com.android.build.gradle.internal.res.Aapt2FromMaven
import com.android.build.gradle.internal.scope.ProjectInfo
import com.android.build.gradle.internal.services.Aapt2Input
import com.android.build.gradle.internal.services.ProjectServices
import com.android.build.gradle.internal.services.getAapt2Executable
import com.android.build.gradle.options.ProjectOptionService
import com.android.build.gradle.options.SyncOptions
import com.android.tools.build.bundletool.commands.BuildApksCommand
import com.android.tools.build.bundletool.model.Aapt2Command
import com.android.tools.build.bundletool.model.Password
import com.android.tools.build.bundletool.model.SigningConfiguration
import com.google.common.util.concurrent.MoreExecutors
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.jeppeman.globallydynamic.gradle.extensions.*
import com.jeppeman.globallydynamic.gradle.extensions.deleteCompletely
import com.jeppeman.globallydynamic.gradle.extensions.unzip
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.security.KeyStore
import java.util.Optional
import java.util.concurrent.ForkJoinPool

abstract class ApkProducerTask : DefaultTask() {
    private val gson: Gson by lazy { GsonBuilder().create() }

    // AGP 3.5 to 3.6 compatibility
    private fun JsonObject.getPropertyCompat(propName: String) =
        get(propName) ?: get("m${propName.capitalize()}")

    @get:Input
    protected abstract val buildMode: BuildApksCommand.ApkBuildMode
    protected abstract fun processApkSet(apkSet: Path)

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    lateinit var bundleDir: File
        private set

    @get:InputFile
    lateinit var signingConfig: File
        private set

    @get:OutputDirectory
    lateinit var outputDir: File
        protected set

    @get:Input
    var signed: Boolean = true
        private set

    @get:Nested
    abstract val aapt2: Aapt2Input

    @get:Input
    lateinit var variantName: String
        private set

    private fun getSigningConfiguration(): SigningConfiguration? {
        val signingConfigJson = signingConfig.takeIf { it.exists() }?.readText() ?: return null
        return if (signingConfigJson.isNotBlank() && signingConfigJson != "null") {
            val signingConfig = gson.fromJson(signingConfigJson, JsonObject::class.java)
            SigningConfiguration.extractFromKeystore(
                Paths.get(signingConfig.getPropertyCompat("storeFile").asString),
                signingConfig.getPropertyCompat("keyAlias").asString,
                Optional.of(Password {
                    KeyStore.PasswordProtection(
                        signingConfig.getPropertyCompat("storePassword")
                            .asString
                            .toCharArray()
                    )
                }),
                Optional.of(Password {
                    KeyStore.PasswordProtection(
                        signingConfig.getPropertyCompat("keyPassword")
                            .asString
                            .toCharArray()
                    )
                })
            )
        } else {
            null
        }
    }


    @TaskAction
    fun doTaskAction() {
        val bundle = bundleDir.listFiles { file -> file.name.contains(".aab") }!!.first()
        val apksOutputPath = outputDir.toPath().resolve("bundle.apks")
        apksOutputPath.deleteCompletely()

        val buildApksCommandBuilder = BuildApksCommand.builder()
            .setExecutorService(MoreExecutors.listeningDecorator(ForkJoinPool.commonPool()))
            .setBundlePath(bundle.toPath())
            .setOutputFile(apksOutputPath)
            .setApkBuildMode(buildMode)
            .setAapt2Command(Aapt2Command.createFromExecutablePath(aapt2.getAapt2Executable().toFile().toPath()))

        if (signed) {
            getSigningConfiguration().let(buildApksCommandBuilder::setSigningConfiguration)
        }

        var debugTempKeystorePath: Path? = null
        val tempFileSuffix = ".temp"
        if (!signed) {
            getDebugKeystorePath()?.takeIf { Files.exists(it) }?.let {
                debugTempKeystorePath = it.parent.resolve("${it.toFile().name}$tempFileSuffix")
                Files.move(it, debugTempKeystorePath!!)
            }
        }

        try {
            processApkSet(buildApksCommandBuilder.build().execute())
        } catch (exception: Exception) {
            throw exception
        } finally {
            apksOutputPath.deleteCompletely()
            debugTempKeystorePath?.takeIf { Files.exists(it) }?.let {
                Files.move(it, it.parent.resolve(it.toFile().name.dropLast(tempFileSuffix.length)))
            }
        }
    }

    abstract class CreationAction<T : ApkProducerTask>(
        applicationVariant: ApplicationVariant,
        private val signed: Boolean
    ) : VariantTaskAction<T>(applicationVariant) {
        override fun execute(task: T) {
            task.variantName = applicationVariant.name
            task.signed = signed
            task.bundleDir = task.project.intermediaryBundleDir(applicationVariant)
            task.signingConfig = task.project.intermediarySigningConfig(applicationVariant)
            createProjectServices(task.project).initializeAapt2Input(task.aapt2)
        }
    }
}

abstract class BuildUniversalApkTask : ApkProducerTask() {
    override val buildMode: BuildApksCommand.ApkBuildMode = BuildApksCommand.ApkBuildMode.UNIVERSAL

    override fun processApkSet(apkSet: Path) {
        val tempDir = Paths.get(outputDir.absolutePath, "temp")
        tempDir.deleteCompletely()
        apkSet.unzip(tempDir.toFile().absolutePath)
        (tempDir.toFile().listFiles() ?: arrayOf()).forEach { file ->
            if (file.extension == "apk") {
                val apk = outputDir.toPath()
                    .resolve("${file.nameWithoutExtension}${if (signed) "" else "-unsigned"}.apk")
                Files.move(file.toPath(), apk, StandardCopyOption.REPLACE_EXISTING)
            }
        }
        tempDir.deleteCompletely()
    }

    class CreationAction(
        applicationVariant: ApplicationVariant,
        private val signed: Boolean
    ) : ApkProducerTask.CreationAction<BuildUniversalApkTask>(applicationVariant, signed) {
        override val name: String
            get() = applicationVariant.getTaskName("build${if (signed) "" else "Unsigned"}UniversalApkFor")

        override fun execute(task: BuildUniversalApkTask) {
            super.execute(task)
            task.outputDir = Paths.get(
                task.project.buildDir.absolutePath,
                "outputs",
                "universal_apk",
                applicationVariant.name
            ).toFile()
        }
    }
}

abstract class BuildBaseApkTask : ApkProducerTask() {
    override val buildMode: BuildApksCommand.ApkBuildMode = BuildApksCommand.ApkBuildMode.DEFAULT

    override fun processApkSet(apkSet: Path) {
        val tempDir = Paths.get(outputDir.absolutePath, "temp")
        tempDir.deleteCompletely()
        apkSet.unzip(tempDir.toFile().absolutePath)
        tempDir.resolve("splits")
            .resolve("base-master.apk")
            .takeIf { Files.exists(it) }?.let { masterApk ->
                val apk = outputDir.toPath()
                    .resolve("${masterApk.toFile().nameWithoutExtension}${if (signed) "" else "-unsigned"}.apk")
                Files.move(masterApk.toFile().toPath(), apk, StandardCopyOption.REPLACE_EXISTING)
            }
        tempDir.deleteCompletely()
    }

    class CreationAction(
        applicationVariant: ApplicationVariant,
        private val signed: Boolean
    ) : ApkProducerTask.CreationAction<BuildBaseApkTask>(applicationVariant, signed) {
        override val name: String
            get() = applicationVariant.getTaskName("build${if (signed) "" else "Unsigned"}BaseApkFor")

        override fun execute(task: BuildBaseApkTask) {
            super.execute(task)
            task.outputDir = Paths.get(
                task.project.buildDir.absolutePath,
                "outputs",
                "base_apk",
                applicationVariant.name
            ).toFile()
        }
    }
}

private const val ENV_VAR_ANDROID_SDK_HOME = "ANDROID_SDK_HOME"
private const val ENV_VAR_USER_HOME = "user.home"
private const val ENV_VAR_HOME = "HOME"

private fun createProjectServices(project: Project): ProjectServices {
    val objectFactory = project.objects
    val logger = project.logger
    val projectPath = project.path
    val projectOptions = ProjectOptionService.RegistrationAction(project).execute().get()
        .projectOptions
    val syncIssueReporter =
        SyncIssueReporterImpl(
            SyncOptions.getModelQueryMode(projectOptions),
            SyncOptions.ErrorFormatMode.HUMAN_READABLE,
            logger
        )
    val deprecationReporter =
        DeprecationReporterImpl(syncIssueReporter, projectOptions, projectPath)
    return ProjectServices(
        issueReporter = syncIssueReporter,
        deprecationReporter = deprecationReporter,
        objectFactory = objectFactory,
        logger = project.logger,
        providerFactory = project.providers,
        projectLayout = project.layout,
        projectInfo = ProjectInfo(project),
        projectOptions = projectOptions,
        buildServiceRegistry = project.gradle.sharedServices,
        maxWorkerCount = project.gradle.startParameter.maxWorkerCount,
        aapt2FromMaven = Aapt2FromMaven.create(project, projectOptions::get),
        lintFromMaven = LintFromMaven.from(
            project = project,
            projectOptions = projectOptions,
            issueReporter = syncIssueReporter
        ),
        fileResolver = project::file,
        configurationContainer = project.configurations,
        dependencyHandler = project.dependencies,
        extraProperties = project.extensions.extraProperties,
        emptyTaskCreator = { name -> project.tasks.register(name) },
        plugins = project.pluginManager,
    )
}

private fun getDebugKeystorePath(): Path? = listOf(
    ENV_VAR_ANDROID_SDK_HOME,
    ENV_VAR_HOME,
    ENV_VAR_USER_HOME
).asSequence().mapNotNull { envVar ->
    System.getenv(envVar)
}.filter { envVar ->
    envVar.isNotBlank()
}.map { envVar ->
    Paths.get(envVar)
}.filter { path ->
    Files.isDirectory(path)
}.map { path ->
    path.resolve(".android").resolve("debug.keystore")
}.filter { keystore ->
    Files.exists(keystore)
}.filter { keystore ->
    Files.isReadable(keystore)
}.firstOrNull()
