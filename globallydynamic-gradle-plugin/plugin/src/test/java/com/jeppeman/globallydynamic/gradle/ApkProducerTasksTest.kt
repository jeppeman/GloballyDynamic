package com.jeppeman.globallydynamic.gradle

import com.google.common.truth.Truth.assertThat
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeSpec
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import java.nio.file.Paths
import java.util.zip.ZipFile
import kotlin.io.path.exists
import kotlin.io.path.fileSize

abstract class ApkProducerTasksTest : BaseTaskTest() {
    override val installTimeFeatureName: String = "installtimefeature"
    override val onDemandFeatureName: String = "ondemandfeature"

    protected val appModuleOutputsDir get() = appModuleProjectDirPath.resolve("build").resolve("outputs")

    override fun beforeEach() {
        val testResPath = Paths.get("src", "test", "resources")
        val keyStoreFile = Paths.get(testResPath.toString(), "test.keystore")
        val keyStorePath = rootProjectDirPath.resolve("test.keystore").apply {
            toFile().writeBytes(keyStoreFile.toFile().inputStream().readBytes())
        }
        val typeSpec = TypeSpec.classBuilder("GloballyDynamicActivity")
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
                <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                    <application />
                </manifest>
            """.trimIndent()
        )

        appModuleBuildFilePath.toFile().writeText(
            """
                plugins {
                    id 'com.android.application'
                    id 'com.jeppeman.globallydynamic'
                }
                
                android {
                    namespace '$BASE_PACKAGE_NAME'
                    
                    compileSdk 32
                    
                    defaultConfig {
                        minSdk 16
                        targetSdk 32
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
                    
                    dynamicFeatures = [':$onDemandFeatureName', ':$installTimeFeatureName']
                }
                
                dependencies {
                    implementation 'com.jeppeman.globallydynamic.android:gplay:${ANDROID_LIB_VERSION}'
                    implementation 'androidx.appcompat:appcompat:1.4.2'
                }
            """.trimIndent()
        )

        onDemandFeatureAndroidManifestFilePath.toFile().writeText(
            """
               <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                    xmlns:dist="http://schemas.android.com/apk/distribution">
                    
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
                    xmlns:dist="http://schemas.android.com/apk/distribution">
                    
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
                                    <dist:min-sdk dist:value="16"/>
                                </dist:conditions>
                            </dist:install-time>
                        </dist:delivery>
                        <dist:fusing dist:include="true" />
                    </dist:module>
                </manifest> 
            """.trimIndent()
        )
    }
}

private const val VERSION_CODE = 1
private const val VARIANT = "debug"

@RunWith(JUnitPlatform::class)
class BuildUniversalApkTaskTest : ApkProducerTasksTest() {
    override val taskName: String = ":app:buildUniversalApkFor${VARIANT.capitalize()}"

    private val producedApk get() = appModuleOutputsDir.resolve("universal_apk")
        .resolve(VARIANT)
        .resolve("universal.apk")

    @Test
    fun `task should produce a universal apk`() {
        val result = runTask()

        assertThat(result.task(taskName)?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(producedApk.exists()).isTrue()
        assertThat(producedApk.fileSize()).isGreaterThan(0L)
        assertThat(ZipFile(producedApk.toFile()).entries().asSequence().any { it.name.matches("META-INF/.+\\.RSA".toRegex()) }).isTrue()
    }
}

@RunWith(JUnitPlatform::class)
class BuildUnsignedUniversalApkTaskTest : ApkProducerTasksTest() {
    override val taskName: String = ":app:buildUnsignedUniversalApkFor${VARIANT.capitalize()}"

    private val producedApk get() = appModuleOutputsDir.resolve("universal_apk")
        .resolve(VARIANT)
        .resolve("universal-unsigned.apk")

    @Test
    fun `task should produce an unsigned universal apk`() {
        val result = runTask()

        assertThat(result.task(taskName)?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(producedApk.exists()).isTrue()
        assertThat(producedApk.fileSize()).isGreaterThan(0L)
        assertThat(ZipFile(producedApk.toFile()).entries().asSequence().any { it.name.matches("META-INF/.+\\.RSA".toRegex()) }).isFalse()
    }
}

@RunWith(JUnitPlatform::class)
class BuildBaseApkTaskTest : ApkProducerTasksTest() {
    override val taskName: String = ":app:buildBaseApkFor${VARIANT.capitalize()}"

    private val producedApk get() = appModuleOutputsDir.resolve("base_apk")
        .resolve(VARIANT)
        .resolve("base-master.apk")

    @Test
    fun `task should produce a base apk`() {
        val result = runTask()

        assertThat(result.task(taskName)?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(producedApk.exists()).isTrue()
        assertThat(producedApk.fileSize()).isGreaterThan(0L)
        assertThat(ZipFile(producedApk.toFile()).entries().asSequence().any { it.name.matches("META-INF/.+\\.RSA".toRegex()) }).isTrue()
    }
}

@RunWith(JUnitPlatform::class)
class BuildUnsignedBaseApkTaskTest : ApkProducerTasksTest() {
    override val taskName: String = ":app:buildUnsignedBaseApkFor${VARIANT.capitalize()}"

    private val producedApk get() = appModuleOutputsDir.resolve("base_apk")
        .resolve(VARIANT)
        .resolve("base-master-unsigned.apk")

    @Test
    fun `task should produce an unsigned base apk`() {
        val result = runTask()

        assertThat(result.task(taskName)?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(producedApk.exists()).isTrue()
        assertThat(producedApk.fileSize()).isGreaterThan(0L)
        assertThat(ZipFile(producedApk.toFile()).entries().asSequence().any { it.name.matches("META-INF/.+\\.RSA".toRegex()) }).isFalse()
    }
}
