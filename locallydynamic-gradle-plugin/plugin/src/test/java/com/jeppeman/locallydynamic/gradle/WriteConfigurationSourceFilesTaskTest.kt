package com.jeppeman.locallydynamic.gradle

import com.android.tools.r8.errors.CompilationError
import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.net.URI
import java.nio.file.Path
import java.util.*
import javax.tools.*

@RunWith(JUnitPlatform::class)
class WriteConfigurationSourceFilesTaskTest : BaseTaskTest() {
    private lateinit var configurationPath: Path
    private lateinit var locallyDynamicBuildConfigPath: Path
    private lateinit var deviceFeatureConditionPath: Path
    private lateinit var moduleConditionsPath: Path
    private lateinit var userCountriesConditionPath: Path
    override val onDemandFeatureName: String = "dynamic"
    override val installTimeFeatureName: String = "installtimefeature"
    override val taskName: String = ":app:writeLocallyDynamicConfigurationSourceFilesFor${VARIANT.capitalize()}"

    override fun beforeEach() {
        configurationPath = appModuleProjectDirPath.resolve("build")
            .resolve("generated")
            .resolve("source")
            .resolve("locallydynamic")
            .resolve(VARIANT)
            .resolve("com")
            .resolve("jeppeman")
            .resolve("locallydynamic")
            .resolve("generated")

        locallyDynamicBuildConfigPath = configurationPath.resolve("LocallyDynamicBuildConfig.java")
        deviceFeatureConditionPath = configurationPath.resolve("DeviceFeatureCondition.java")
        moduleConditionsPath = configurationPath.resolve("ModuleConditions.java")
        userCountriesConditionPath = configurationPath.resolve("UserCountriesCondition.java")
        
        appModuleAndroidManifestFilePath.toFile().writeText(
            """
                <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                    package="$BASE_PACKAGE_NAME">
                    <application>
                        <activity android:name="$MAIN_ACTIVITY_NAME" >
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
                                serverUrl = "$SERVER_URL"
                                username = "$USERNAME"
                                password = "$PASSWORD"
                                throttleDownloadBy = $THROTTLE_DOWNLOAD_BY
                            }
                            
                            matchingFallbacks = ['debug']
                        }
                    }
                    
                    dynamicFeatures = [':$onDemandFeatureName', ':$installTimeFeatureName']
                }
                
                dependencies {
                    implementation 'com.jeppeman.locallydynamic:locallydynamic:0.3'
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
                        dist:title="Dynamic">
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
                        dist:title="Install Time">
                        <dist:delivery>
                            <dist:install-time>
                                <dist:conditions>
                                    <dist:user-countries dist:exclude="false">
                                        <dist:country dist:code="${INSTALL_TIME_USER_COUNTRIES[0]}"/>
                                        <dist:country dist:code="${INSTALL_TIME_USER_COUNTRIES[1]}"/>
                                    </dist:user-countries>
                                    <dist:device-feature dist:name="${INSTALL_TIME_DEVICE_FEATURES[0]}"/>
                                    <dist:device-feature dist:name="${INSTALL_TIME_DEVICE_FEATURES[1]}"/>
                                    <dist:min-sdk dist:value="$INSTALL_TIME_MIN_SDK"/>
                                </dist:conditions>
                            </dist:install-time>
                        </dist:delivery>
                        <dist:fusing dist:include="true" />
                    </dist:module>
                </manifest> 
            """.trimIndent()
        )
    }

    private fun compileGeneratedFiles(): Any {
        val compiler = ToolProvider.getSystemJavaCompiler()
        val fm = compiler.getStandardFileManager(null, Locale.ENGLISH, Charsets.UTF_8)
        val fileManager = SimpleJavaFileManager(fm)
        val files = fm.getJavaFileObjects(
            deviceFeatureConditionPath.toFile(),
            moduleConditionsPath.toFile(),
            userCountriesConditionPath.toFile(),
            locallyDynamicBuildConfigPath.toFile()
        )
        val stringWriter = StringWriter()
        val writer = PrintWriter(stringWriter)
        val task = compiler.getTask(writer, fileManager, null, null, null, files)
        val success = task.call()
        return if (success) {
            val classLoader = CompiledClassLoader(fileManager.generatedOutputFiles)
            classLoader.loadClass(LOCALLY_DYNAMIC_BUILD_CONFIG_CLASS).newInstance()
        } else {
            throw CompilationError(stringWriter.toString())
        }
    }

    @Test
    fun whenConfigurationIsProvidedThroughDsl_task_shouldGenerateFilesAccordingly() {
        val result = runTask()
        val locallyDynamicBuildConfig = compileGeneratedFiles()
        val installTimeFeatures = locallyDynamicBuildConfig.invokeMethod<Map<String, Any>>("getInstallTimeFeatures")
        val installTimeFeatureModuleConditions = installTimeFeatures[installTimeFeatureName] ?: error("")
        val installTimeMinSdk = installTimeFeatureModuleConditions.invokeMethod<Int>("getMinSdkCondition")
        val installTimeUserCountriesCondition = installTimeFeatureModuleConditions.invokeMethod<Any>("getUserCountriesCondition")
        val installTimeUserCountries = installTimeUserCountriesCondition.invokeMethod<Array<String>>("getCountries").toList()
        val installTimeUserCountriesExclude = installTimeUserCountriesCondition.invokeMethod<Boolean>("getExclude")
        val installTimeDeviceFeatureConditions = installTimeFeatureModuleConditions.invokeMethod<Array<Any>>("getDeviceFeatureConditions")
            .map { it.invokeMethod<String>("getFeatureName") }

        assertThat(result.task(taskName)?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(installTimeMinSdk).isEqualTo(INSTALL_TIME_MIN_SDK)
        assertThat(installTimeUserCountries).isEqualTo(INSTALL_TIME_USER_COUNTRIES)
        assertThat(installTimeUserCountriesExclude).isFalse()
        assertThat(installTimeDeviceFeatureConditions).isEqualTo(INSTALL_TIME_DEVICE_FEATURES)
        assertThat(locallyDynamicBuildConfig.invokeMethod<String>("getMainActivityFullyQualifiedName")).isEqualTo(MAIN_ACTIVITY_NAME)
        assertThat(locallyDynamicBuildConfig.invokeMethod<String>("getServerUrl")).isEqualTo(SERVER_URL)
        assertThat(locallyDynamicBuildConfig.invokeMethod<String>("getUsername")).isEqualTo(USERNAME)
        assertThat(locallyDynamicBuildConfig.invokeMethod<String>("getPassword")).isEqualTo(PASSWORD)
        assertThat(locallyDynamicBuildConfig.invokeMethod<String>("getVariantName")).isEqualTo(VARIANT)
        assertThat(locallyDynamicBuildConfig.invokeMethod<Long>("getThrottleDownloadBy")).isEqualTo(THROTTLE_DOWNLOAD_BY)
        assertThat(locallyDynamicBuildConfig.invokeMethod<Int>("getVersionCode")).isEqualTo(VERSION_CODE)
        assertThat(locallyDynamicBuildConfig.invokeMethod<Array<String>>("getOnDemandFeatures")).isEqualTo(arrayOf(onDemandFeatureName))
    }

    @Test
    fun whenConfigurationIsProvidedThroughArguments_task_shouldGenerateFilesWithAccordingly() {
        val serverUrlArgument = "http://program.argument"
        val usernameArgument = "usernameArgument"
        val passwordArgument = "passwordArgument"
        val throttleByArgument = 1000

        val result = runTask(
            "-PlocallyDynamic.serverUrl=$serverUrlArgument",
            "-PlocallyDynamic.username=$usernameArgument",
            "-PlocallyDynamic.password=$passwordArgument",
            "-PlocallyDynamic.throttleDownloadBy=$throttleByArgument")
        val locallyDynamicBuildConfig = compileGeneratedFiles()

        assertThat(result.task(taskName)?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(locallyDynamicBuildConfig.invokeMethod<String>("getServerUrl")).isEqualTo(serverUrlArgument)
        assertThat(locallyDynamicBuildConfig.invokeMethod<String>("getUsername")).isEqualTo(usernameArgument)
        assertThat(locallyDynamicBuildConfig.invokeMethod<String>("getPassword")).isEqualTo(passwordArgument)
        assertThat(locallyDynamicBuildConfig.invokeMethod<Long>("getThrottleDownloadBy")).isEqualTo(throttleByArgument)
    }

    @Test
    fun whenConfigurationIsProvidedThroughFile_task_shouldGenerateFilesAccordingly() {
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
                                throttleDownloadBy = $THROTTLE_DOWNLOAD_BY
                            }
                            
                            matchingFallbacks = ['debug']
                        }
                    }
                    
                    dynamicFeatures = [':$onDemandFeatureName', ':$installTimeFeatureName']
                }
                
                dependencies {
                    implementation 'com.jeppeman.locallydynamic:locallydynamic:0.3'
                    implementation 'androidx.appcompat:appcompat:1.1.0'
                }
            """.trimIndent()
        )
        val locallyDynamicServerInfo = LocallyDynamicServerInfo(
            serverUrl = "http://server.url",
            username = "username",
            password = "password"
        )
        appModuleProjectDirPath.resolve("build")
            .resolve("locallydynamic")
            .apply { toFile().mkdirs() }
            .resolve("server_info.json")
            .toFile()
            .writeText(Gson().toJson(locallyDynamicServerInfo))

        val result = runTask()
        val locallyDynamicBuildConfig = compileGeneratedFiles()

        assertThat(result.task(taskName)?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(locallyDynamicBuildConfig.invokeMethod<String>("getServerUrl")).isEqualTo(locallyDynamicServerInfo.serverUrl)
        assertThat(locallyDynamicBuildConfig.invokeMethod<String>("getUsername")).isEqualTo(locallyDynamicServerInfo.username)
        assertThat(locallyDynamicBuildConfig.invokeMethod<String>("getPassword")).isEqualTo(locallyDynamicServerInfo.password)
        assertThat(locallyDynamicBuildConfig.invokeMethod<Long>("getThrottleDownloadBy")).isEqualTo(THROTTLE_DOWNLOAD_BY)
    }

    companion object {
        private const val CLASS_PREFIX = "$BASE_PACKAGE_NAME.generated"
        private const val LOCALLY_DYNAMIC_BUILD_CONFIG_CLASS = "$CLASS_PREFIX.LocallyDynamicBuildConfig"
        private const val MAIN_ACTIVITY_NAME = "$BASE_PACKAGE_NAME.LocallyDynamicActivity"
        private const val SERVER_URL = "http://locallydynamic.io"
        private const val USERNAME = "username"
        private const val PASSWORD = "password"
        private const val THROTTLE_DOWNLOAD_BY = 5000
        private const val VARIANT = "instrumentation"
        private const val VERSION_CODE = 5
        private const val INSTALL_TIME_MIN_SDK = 24
        private val INSTALL_TIME_USER_COUNTRIES = listOf("CN", "HK")
        private val INSTALL_TIME_DEVICE_FEATURES = listOf("android.hardware.camera.ar", "android.hardware.camera.vr")
    }
}

private fun <T : Any> Any.invokeMethod(method: String): T =
    this::class.java.getDeclaredMethod(method).invoke(this) as T

private class ClassJavaFileObject(val className: String, kind: JavaFileObject.Kind) : SimpleJavaFileObject(
    URI.create("mem:///" + className.replace('.', '/') + kind.extension), kind
) {
    private val outputStream = ByteArrayOutputStream()

    override fun openOutputStream(): OutputStream {
        return outputStream
    }

    fun getBytes(): ByteArray = outputStream.toByteArray()
}

private class CompiledClassLoader(private val files: MutableList<ClassJavaFileObject>) : ClassLoader() {
    override fun findClass(name: String?): Class<*> {
        val iterator = files.iterator()
        while (iterator.hasNext()) {
            val file = iterator.next()
            if (file.className == name) {
                iterator.remove()
                val bytes = file.getBytes()
                return super.defineClass(name, bytes, 0, bytes.size)
            }
        }

        return super.findClass(name)
    }
}

private class SimpleJavaFileManager(
    javaFileManager: JavaFileManager
) : ForwardingJavaFileManager<JavaFileManager>(
    javaFileManager
) {
    val generatedOutputFiles = mutableListOf<ClassJavaFileObject>()

    override fun getJavaFileForOutput(location: JavaFileManager.Location?, className: String?, kind: JavaFileObject.Kind?, sibling: FileObject?): JavaFileObject {
        val classJavaFileObject = ClassJavaFileObject(className!!, kind!!)
        generatedOutputFiles.add(classJavaFileObject)
        return classJavaFileObject
    }
}