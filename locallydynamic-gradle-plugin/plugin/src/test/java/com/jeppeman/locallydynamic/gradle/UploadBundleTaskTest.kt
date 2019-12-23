package com.jeppeman.locallydynamic.gradle

import com.google.common.truth.Truth.assertThat
import com.jeppeman.locallydynamic.server.LocalStorageBackend
import com.jeppeman.locallydynamic.server.LocallyDynamicServer
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeSpec
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import java.nio.file.Paths

@RunWith(JUnitPlatform::class)
class UploadBundleTaskTest : BaseTaskTest() {
    private val locallyDynamicServer: LocallyDynamicServer by lazy {
        LocallyDynamicServer(
            LocallyDynamicServer.Configuration.builder()
                .setPort(8080)
                .setStorageBackend(
                    LocalStorageBackend.builder()
                        .setBaseStoragePath(rootProjectDirPath)
                        .build()
                )
                .setUsername("username")
                .setPassword("password")
                .build()
        )
    }
    override val installTimeFeatureName: String = "installtimefeature"
    override val onDemandFeatureName: String = "ondemandfeature"
    override val taskName: String = ":app:uploadLocallyDynamicBundle${VARIANT.capitalize()}"

    override fun beforeEach() {
        val testResPath = Paths.get("src", "test", "resources")
        val keyStoreFile = Paths.get(testResPath.toString(), "test.keystore")
        val keyStorePath = rootProjectDirPath.resolve("test.keystore").apply {
            toFile().writeBytes(keyStoreFile.toFile().inputStream().readBytes())
        }
        val typeSpec = TypeSpec.classBuilder("LocallyDynamicActivity")
            .superclass(ClassName.get("androidx.appcompat.app", "AppCompatActivity"))
            .build()

        val stringsFile = appModuleSourceDir.resolve("res")
            .resolve("values")
            .apply { toFile().mkdirs() }
            .resolve("strings.xml")

        stringsFile.toFile().writeText(
            """
                <resources>
                    <string name="title_installtimefeature">Install Time Feature</string>
                    <string name="title_ondemandfeature">On Demand Feature</string>
                </resources>
            """.trimIndent()
        )

        JavaFile.builder(BASE_PACKAGE_NAME, typeSpec)
            .build()
            .writeTo(appModuleSourceDir)

        appModuleAndroidManifestFilePath.toFile().writeText(
            """
                <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                    package="$BASE_PACKAGE_NAME">
                    <application />
                </manifest>
            """.trimIndent()
        )

        appModuleBuildFilePath.toFile().writeText(
            """
                plugins {
                    id 'com.android.application'
                    id 'com.jeppeman.locallydynamic'
                }
                
                android {
                    defaultConfig {
                        compileSdkVersion 29
                        minSdkVersion 29
                        targetSdkVersion 29
                        versionCode $VERSION_CODE
                    }
                    
                    signingConfigs {
                        debug {
                            storeFile file('$keyStorePath')
                            storePassword 'android'
                            keyAlias 'androiddebugkey'
                            keyPassword 'android'
                        }
                    }
                    
                    buildTypes {
                        $VARIANT {
                            locallyDynamic {
                                enabled = true
                                serverUrl = "http://localhost:8080"
                                username = "username"
                                password = "password"
                            }
                        }
                    }
                    
                    dynamicFeatures = [':$onDemandFeatureName', ':$installTimeFeatureName']
                }
                
                dependencies {
                    implementation 'com.jeppeman.locallydynamic:locallydynamic-debug:0.1'
                    implementation 'androidx.appcompat:appcompat:1.1.0'
                }
            """.trimIndent()
        )

        onDemandFeatureAndroidManifestFilePath.toFile().writeText(
            """
               <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                    xmlns:dist="http://schemas.android.com/apk/distribution"
                    package="$BASE_PACKAGE_NAME">
                    
                    <application/>
                    
                    <dist:module
                        dist:instant="false"
                        dist:title="@string/title_ondemandfeature">
                        <dist:delivery>
                            <dist:on-demand />
                        </dist:delivery>
                        <dist:fusing dist:include="true" />
                    </dist:module>
                </manifest> 
            """.trimIndent()
        )

        installTimeFeatureAndroidManifestFilePath.toFile().writeText(
            """
               <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                    xmlns:dist="http://schemas.android.com/apk/distribution"
                    package="$BASE_PACKAGE_NAME">
                    
                    <application/>
                    
                    <dist:module
                        dist:instant="false"
                        dist:title="@string/title_installtimefeature">
                        <dist:delivery>
                            <dist:install-time>
                                <dist:conditions>
                                    <dist:user-countries dist:exclude="false">
                                        <dist:country dist:code="CN"/>
                                        <dist:country dist:code="HK"/>
                                    </dist:user-countries>
                                    <dist:device-feature dist:name="android.hardware.feature.ar"/>
                                    <dist:device-feature dist:name="android.hardware.feature.vr"/>
                                    <dist:min-sdk dist:value="29"/>
                                </dist:conditions>
                            </dist:install-time>
                        </dist:delivery>
                        <dist:fusing dist:include="true" />
                    </dist:module>
                </manifest> 
            """.trimIndent()
        )
    }

    @Test
    fun whenServerIsReachable_task_shouldUploadBundleAndSigningConfig() {
        locallyDynamicServer.start()

        val result = runTask()

        val bundleFile = rootProjectDirPath.resolve("${BASE_PACKAGE_NAME}_${VARIANT}_$VERSION_CODE.aab").toFile()
        val signingConfigFile = rootProjectDirPath.resolve("${BASE_PACKAGE_NAME}_${VARIANT}_$VERSION_CODE.json").toFile()
        val keystoreFile = rootProjectDirPath.resolve("${BASE_PACKAGE_NAME}_${VARIANT}_$VERSION_CODE.keystore").toFile()
        assertThat(result.task(taskName)?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.task(":app:package${VARIANT.capitalize()}Bundle")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(bundleFile.exists()).isTrue()
        assertThat(signingConfigFile.exists()).isTrue()
        assertThat(keystoreFile.exists()).isTrue()
        locallyDynamicServer.stop()
    }

    @Test
    fun whenNoServerUrlIsPresent_task_shouldThrow() {
        appModuleBuildFilePath.toFile().writeText(
            """
                plugins {
                    id 'com.android.application'
                    id 'com.jeppeman.locallydynamic'
                }
                
                android {
                    defaultConfig {
                        compileSdkVersion 29
                        minSdkVersion 29
                        targetSdkVersion 29
                        versionCode $VERSION_CODE
                    }
                    
                    buildTypes {
                        $VARIANT {
                            locallyDynamic {
                                enabled = true
                                username = "username"
                                password = "password"
                            }
                        }
                    }
                    
                    dynamicFeatures = [':$onDemandFeatureName', ':$installTimeFeatureName']
                }
                
                dependencies {
                    implementation 'com.jeppeman.locallydynamic:locallydynamic-debug:0.1'
                    implementation 'androidx.appcompat:appcompat:1.1.0'
                }
            """.trimIndent()
        )

        val executable = {
            runTask()
            Unit
        }

        val thrown = assertThrows<UnexpectedBuildFailure>(executable)
        assertThat(thrown.buildResult.task(taskName)?.outcome).isEqualTo(TaskOutcome.FAILED)
        assertThat(thrown.buildResult.output).contains("No server url for locallydynamic found")
    }

    @Test
    fun whenServerIsNotReachable_task_shouldThrow() {
        val executable = {
            runTask()
            Unit
        }

        val thrown = assertThrows<UnexpectedBuildFailure>(executable)
        assertThat(thrown.buildResult.task(taskName)?.outcome).isEqualTo(TaskOutcome.FAILED)
        assertThat(thrown.buildResult.output).contains("Failed to upload bundle, make sure the server is running and is reachable at")
    }

    companion object {
        private const val VARIANT = "debug"
        private const val VERSION_CODE = 23
    }
}