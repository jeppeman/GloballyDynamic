package com.jeppeman.globallydynamic.gradle

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

const val ANDROID_LIB_VERSION = "1.3.0"

abstract class BaseTaskTest {
    @TempDir
    lateinit var rootProjectDirPath: Path
    lateinit var rootProjectBuildFilePath: Path
    lateinit var appModuleProjectDirPath: Path
    lateinit var appModuleBuildFilePath: Path
    lateinit var appModuleSourceDir: Path
    lateinit var appModuleAndroidManifestFilePath: Path
    lateinit var onDemandFeatureProjectDirPath: Path
    lateinit var onDemandFeatureProjectBuildFilePath: Path
    lateinit var onDemandFeatureAndroidManifestFilePath: Path
    lateinit var installTimeFeatureDirPath: Path
    lateinit var installTimeFeatureBuildFilePath: Path
    lateinit var installTimeFeatureAndroidManifestFilePath: Path
    lateinit var settingsFilePath: Path
    abstract val onDemandFeatureName: String
    abstract val installTimeFeatureName: String
    abstract val taskName: String

    @BeforeEach
    fun setUp() {
        rootProjectDirPath.resolve("gradle.properties").toFile().writeText(
                """
                android.useAndroidX=true
            """.trimIndent()
        )
        rootProjectBuildFilePath = rootProjectDirPath.resolve("build.gradle")
        appModuleProjectDirPath = rootProjectDirPath.resolve("app").apply { toFile().mkdirs() }
        appModuleBuildFilePath = appModuleProjectDirPath.resolve("build.gradle")
        appModuleSourceDir = appModuleProjectDirPath.resolve("src")
                .resolve("main")
                .apply { toFile().mkdirs() }
        appModuleAndroidManifestFilePath = appModuleSourceDir.resolve("AndroidManifest.xml")
        onDemandFeatureProjectDirPath = rootProjectDirPath.resolve(onDemandFeatureName).apply { toFile().mkdirs() }
        onDemandFeatureProjectBuildFilePath = onDemandFeatureProjectDirPath.resolve("build.gradle")
        onDemandFeatureAndroidManifestFilePath = onDemandFeatureProjectDirPath.resolve("src")
                .resolve("main")
                .apply { toFile().mkdirs() }
                .resolve("AndroidManifest.xml")
        installTimeFeatureDirPath = rootProjectDirPath.resolve(installTimeFeatureName).apply { toFile().mkdirs() }
        installTimeFeatureBuildFilePath = installTimeFeatureDirPath.resolve("build.gradle")
        installTimeFeatureAndroidManifestFilePath = installTimeFeatureDirPath.resolve("src")
                .resolve("main")
                .apply { toFile().mkdirs() }
                .resolve("AndroidManifest.xml")

        settingsFilePath = rootProjectDirPath.resolve("settings.gradle")

        rootProjectBuildFilePath.toFile().writeText(
                """
                buildscript {
                    repositories {
                        google()
                        mavenCentral()
                        jcenter()
                    }
                }
                
                subprojects { 
                    repositories {
                        google()
                        mavenCentral()
                        jcenter()
                    }
                }
            """.trimIndent()
        )

        appModuleBuildFilePath.toFile().writeText(
            """
                
            """.trimIndent()
        )

        installTimeFeatureBuildFilePath.toFile().writeText(
                """
                plugins {
                    id 'com.android.dynamic-feature'
                }
                
                android {
                    namespace '$BASE_PACKAGE_NAME.installtime'
                    
                    defaultConfig {
                        compileSdkVersion 32
                        minSdkVersion 16
                        targetSdkVersion 32
                    }
                }
                
                dependencies {
                    implementation project(':app')
                }
            """.trimIndent()
        )

        installTimeFeatureAndroidManifestFilePath.toFile().writeText(
                """
              <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                    <application/>
              </manifest> 
            """.trimIndent()
        )

        onDemandFeatureProjectBuildFilePath.toFile().writeText(
                """
                plugins {
                    id 'com.android.dynamic-feature'
                }
                
                android {
                    namespace '$BASE_PACKAGE_NAME.ondemand'
                    
                    defaultConfig {
                        compileSdkVersion 32
                        minSdkVersion 16
                        targetSdkVersion 32
                    }
                }
                
                dependencies {
                    implementation project(':app')
                }
            """.trimIndent()
        )

        onDemandFeatureAndroidManifestFilePath.toFile().writeText(
                """
               <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                    <application/>
                </manifest> 
            """.trimIndent()
        )

        settingsFilePath.toFile().writeText(
                """
                include ':app', ':$onDemandFeatureName', ':$installTimeFeatureName'
            """.trimIndent()
        )

        beforeEach()
    }

    abstract fun beforeEach()

    protected fun runTask(vararg args: String): BuildResult = GradleRunner.create()
            .withArguments(taskName, "--stacktrace", *args)
            .withProjectDir(rootProjectDirPath.toFile())
            .withPluginClasspath()
            .build().apply {
                println(this.output)
            }

    companion object {
        const val BASE_PACKAGE_NAME = "com.jeppeman.globallydynamic.test"
    }
}