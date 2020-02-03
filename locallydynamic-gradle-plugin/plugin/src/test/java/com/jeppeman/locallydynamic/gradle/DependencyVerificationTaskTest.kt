package com.jeppeman.locallydynamic.gradle

import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith

@RunWith(JUnitPlatform::class)
class DependencyVerificationTaskTest : BaseTaskTest() {
    override val onDemandFeatureName: String = "dynamic"
    override val installTimeFeatureName: String = "installtimefeature"
    override val taskName: String = ":app:verifyLocallyDynamicDependenciesForDebug"

    override fun beforeEach() {
        appModuleAndroidManifestFilePath.toFile().writeText(
            """
                <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                    package="com.jeppeman.locallydynamic">
                    <application android:name=".LocallyDynamicApplication" />
                </manifest>
            """.trimIndent()
        )
    }

    @Test
    fun whenAndroidDependencyIsDeclaredButPluginIsNotApplied_verifyLocallyDynamicDependenciesForDebug_shouldThrow() {
        appModuleBuildFilePath.toFile().writeText(
            """
                plugins {
                    id 'com.android.application'
                    id 'com.jeppeman.locallydynamic'
                }
                
                android {
                    compileSdkVersion 23
                    
                    dynamicFeatures = [':$onDemandFeatureName']
                }
                
                dependencies {
                    implementation 'com.jeppeman.locallydynamic:locallydynamic-debug:0.2'
                }
            """.trimIndent()
        )

        val result = runTask()

        assertThat(result.task(":app:verifyLocallyDynamicDependenciesForDebug")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("com.jeppeman.locallydynamic:locallydynamic-debug is included as a dependency")
    }

    @Test
    fun whenAndroidDependencyIsDeclaredButDslIsNotEnabled_verifyLocallyDynamicDependenciesForDebug_shouldThrow() {
        appModuleBuildFilePath.toFile().writeText(
            """
                plugins {
                    id 'com.android.application'
                    id 'com.jeppeman.locallydynamic'
                }
                
                android {
                    compileSdkVersion 23
                    
                    buildTypes {
                        debug {
                            locallyDynamic {
                                enabled = false
                            }
                        }
                    }
                    
                    dynamicFeatures = [':$onDemandFeatureName']
                }
                
                dependencies {
                    implementation 'com.jeppeman.locallydynamic:locallydynamic-debug:0.2'
                }
            """.trimIndent()
        )

        val result = runTask()

        assertThat(result.task(":app:verifyLocallyDynamicDependenciesForDebug")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("com.jeppeman.locallydynamic:locallydynamic-debug is included as a dependency")
    }

    @Test
    fun whenAndroidDependencyIsDeclaredAndDslIsEnabled_verifyLocallyDynamicDependenciesForDebug_shouldSucceed() {
        appModuleBuildFilePath.toFile().writeText(
            """
                plugins {
                    id 'com.android.application'
                    id 'com.jeppeman.locallydynamic'
                }
                
                android {
                    compileSdkVersion 23
                    
                    buildTypes {
                        debug {
                            locallyDynamic {
                                enabled = true
                            }
                        }
                    }
                    
                    dynamicFeatures = [':$onDemandFeatureName']
                }
                
                dependencies {
                    implementation 'com.jeppeman.locallydynamic:locallydynamic-debug:0.2'
                }
            """.trimIndent()
        )

        val result = runTask()

        assertThat(result.task(":app:verifyLocallyDynamicDependenciesForDebug")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }

    @Test
    fun whenDslIsEnabledButDependencyIsNotDeclared_verifyLocallyDynamicDependenciesForDebug_shouldThrow() {
        appModuleBuildFilePath.toFile().writeText(
            """
                plugins {
                    id 'com.android.application'
                    id 'com.jeppeman.locallydynamic'
                }
                
                android {
                    compileSdkVersion 23
                    
                    buildTypes {
                        debug {
                            locallyDynamic {
                                enabled = true
                            }
                        }
                    }
                    
                    dynamicFeatures = [':$onDemandFeatureName']
                }
                
                dependencies {
                    implementation 'androidx.appcompat:appcompat:1.1.0'
                }
            """.trimIndent()
        )

        val result = runTask()

        assertThat(result.task(":app:verifyLocallyDynamicDependenciesForDebug")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("com.jeppeman.locallydynamic:locallydynamic-debug must be declared as a dependency")
    }
}