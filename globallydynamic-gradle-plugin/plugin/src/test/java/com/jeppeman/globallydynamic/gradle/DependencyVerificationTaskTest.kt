package com.jeppeman.globallydynamic.gradle

import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith

@RunWith(JUnitPlatform::class)
class DependencyVerificationTaskTest : BaseTaskTest() {
    override val onDemandFeatureName: String = "dynamic"
    override val installTimeFeatureName: String = "installtimefeature"
    override val taskName: String = ":app:verifyGloballyDynamicDependenciesForDebug"

    override fun beforeEach() {
        appModuleAndroidManifestFilePath.toFile().writeText(
            """
                <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                    package="com.jeppeman.globallydynamic">
                    <application android:name=".GloballyDynamicApplication" />
                </manifest>
            """.trimIndent()
        )
    }

    @Test
    fun whenAndroidDependencyIsDeclaredAndDslIsEnabled_verifyGloballyDynamicDependenciesForDebug_shouldSucceed() {
        appModuleBuildFilePath.toFile().writeText(
            """
                plugins {
                    id 'com.android.application'
                    id 'com.jeppeman.globallydynamic'
                }
                
                android {
                    compileSdkVersion 23
                    
                    globallyDynamicServers {
                        server {
                            applyToBuildVariants 'debug'
                        }
                    }
                    
                    dynamicFeatures = [':$onDemandFeatureName']
                }
                
                dependencies {
                    implementation 'com.jeppeman.globallydynamic.android:selfhosted:0.1-SNAPSHOT'
                }
            """.trimIndent()
        )

        val result = runTask()

        assertThat(result.task(":app:verifyGloballyDynamicDependenciesForDebug")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }

    @Test
    fun whenDslIsEnabledButDependencyIsNotDeclared_verifyGloballyDynamicDependenciesForDebug_shouldThrow() {
        appModuleBuildFilePath.toFile().writeText(
            """
                plugins {
                    id 'com.android.application'
                    id 'com.jeppeman.globallydynamic'
                }
                
                android {
                    compileSdkVersion 23
                    
                    globallyDynamicServers {
                        server {
                            applyToBuildVariants 'debug'
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

        assertThat(result.task(":app:verifyGloballyDynamicDependenciesForDebug")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("must be declared as a dependency")
    }
}