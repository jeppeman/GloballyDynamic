package com.jeppeman.globallydynamic.gradle

import com.google.common.truth.Truth.assertThat
import com.jeppeman.globallydynamic.gradle.extensions.deleteCompletely
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith

@RunWith(JUnitPlatform::class)
class GloballyDynamicPluginTest : BaseTaskTest() {
    override val onDemandFeatureName: String = "ondemandfeature"
    override val installTimeFeatureName: String = "installtimefeature"
    override val taskName: String = ":app:tasks"

    override fun beforeEach() = Unit

    @Test
    fun whenAndroidPluginIsMissing_plugin_shouldThrow() {
        appModuleBuildFilePath.toFile().writeText(
            """
                plugins {
                    id 'com.jeppeman.globallydynamic'
                }

                dependencies {
                    implementation 'com.jeppeman.globallydynamic.android:selfhosted:1.0.0'
                    implementation 'androidx.appcompat:appcompat:1.1.0'
                }
            """.trimIndent()
        )

        val executable = {
            runTask("--all")
            Unit
        }

        val thrown = assertThrows<UnexpectedBuildFailure>(executable)
        assertThat(thrown.buildResult.output).contains("Missing plugin com.android.application")
    }

    @Test
    fun whenNoDynamicFeaturesArePresent_plugin_shouldNotAddAnyTasks() {
        appModuleBuildFilePath.toFile().writeText(
            """
                plugins {
                    id 'com.android.application'
                    id 'com.jeppeman.globallydynamic'
                }
                
                android {
                    compileSdkVersion 23
                }

                dependencies {
                    implementation 'com.jeppeman.globallydynamic.android:selfhosted:1.0.0'
                    implementation 'androidx.appcompat:appcompat:1.1.0'
                }
            """.trimIndent()
        )
        onDemandFeatureProjectDirPath.deleteCompletely()
        installTimeFeatureDirPath.deleteCompletely()
        settingsFilePath.toFile().writeText(
            """
                include ':app'
            """.trimIndent()
        )

        val result = runTask("--all")

        assertThat(result.task(taskName)?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(TASKS.none { task -> result.output.contains(task) }).isTrue()
    }

    @Test
    fun whenNoConfigurationIsProvided_plugin_shouldNotAddAnyTasks() {
        appModuleBuildFilePath.toFile().writeText(
            """
                plugins {
                    id 'com.android.application'
                    id 'com.jeppeman.globallydynamic'
                }
                
                android {
                    defaultConfig {
                        compileSdkVersion 29
                        minSdkVersion 29
                        targetSdkVersion 29
                        versionCode 23
                    }
                    
                    dynamicFeatures = [':$onDemandFeatureName', ':$installTimeFeatureName']
                }
                
                dependencies {
                    implementation 'androidx.appcompat:appcompat:1.1.0'
                }
            """.trimIndent()
        )

        val result = runTask("--all")

        assertThat(result.task(taskName)?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(TASKS.none { task -> result.output.contains(task) }).isTrue()
    }

    @Test
    fun whenNoMatchingVariantIsFound_plugin_shouldThrow() {
        appModuleBuildFilePath.toFile().writeText(
            """
                plugins {
                    id 'com.android.application'
                    id 'com.jeppeman.globallydynamic'
                }
                
                android {
                    defaultConfig {
                        compileSdkVersion 29
                        minSdkVersion 29
                        targetSdkVersion 29
                        versionCode 23
                    }
                    
                    buildTypes {
                        instrumentation {
                        }
                    }
                    
                    globallyDynamicServers {
                        server {
                            serverUrl = "http://localhost:8080"
                            username = "username"
                            password = "password"
                            applyToBuildVariants 'instrumentation'
                        }
                    }
                    
                    dynamicFeatures = [':$onDemandFeatureName', ':$installTimeFeatureName']
                }
                
                dependencies {
                    implementation 'com.jeppeman.globallydynamic.android:selfhosted:1.0.0'
                    implementation 'androidx.appcompat:appcompat:1.1.0'
                }
            """.trimIndent()
        )

        val executable = {
            runTask("--all")
            Unit
        }

        val thrown = assertThrows<UnexpectedBuildFailure>(executable)
        assertThat(thrown.buildResult.output).contains("Could not find a matching variant for instrumentation")
    }

    @Test
    fun whenConfigurationIsPresent_plugin_shouldAddAllTasks() {
        appModuleBuildFilePath.toFile().writeText(
            """
                plugins {
                    id 'com.android.application'
                    id 'com.jeppeman.globallydynamic'
                }
                
                android {
                    defaultConfig {
                        compileSdkVersion 29
                        minSdkVersion 29
                        targetSdkVersion 29
                        versionCode 23
                    }
                    
                    buildTypes {
                        instrumentation {
                            matchingFallbacks = ['debug']
                        } 
                    }
                    
                    globallyDynamicServers {
                        server {
                            serverUrl = "http://localhost:8080"
                            username = "username"
                            password = "password"
                            applyToBuildVariants 'instrumentation'
                        }
                    }
                    
                    dynamicFeatures = [':$onDemandFeatureName', ':$installTimeFeatureName']
                }
                
                dependencies {
                    implementation 'com.jeppeman.globallydynamic.android:selfhosted:1.0.0'
                    implementation 'androidx.appcompat:appcompat:1.1.0'
                }
            """.trimIndent()
        )
        appModuleAndroidManifestFilePath.toFile().writeText(
            """
                <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                    package="$BASE_PACKAGE_NAME">
                    <application>
                        <activity android:name=".GloballyDynamicActivity">
                            <intent-filter>
                                <action android:name="android.intent.action.VIEW" />
                                <action android:name="android.intent.action.MAIN" />
                
                                <category android:name="android.intent.category.LAUNCHER" />
                            </intent-filter>
                        </activity>
                    </application>
                </manifest>
            """.trimIndent()
        )

        val result = runTask("--all")

        assertThat(result.task(taskName)?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains(DEPENDENCY_VERIFICATION_TASK)
        assertThat(result.output).contains(WRITE_CONFIGURATION_SOURCE_FILES_TASK)
        assertThat(result.output).contains(UPLOAD_APK_SET_TASK)
    }

    companion object {
        private const val WRITE_CONFIGURATION_SOURCE_FILES_TASK = "writeGloballyDynamicConfigurationSourceFilesForInstrumentation"
        private const val UPLOAD_APK_SET_TASK = "uploadGloballyDynamicBundleInstrumentation"
        private const val DEPENDENCY_VERIFICATION_TASK = "verifyGloballyDynamicDependenciesForInstrumentation"

        private val TASKS = listOf(
            WRITE_CONFIGURATION_SOURCE_FILES_TASK,
            UPLOAD_APK_SET_TASK,
            DEPENDENCY_VERIFICATION_TASK
        )
    }
}