package com.jeppeman.globallydynamic.gradle

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
    private lateinit var globallyDynamicBuildConfigPath: Path
    private lateinit var deviceFeatureConditionPath: Path
    private lateinit var moduleConditionsPath: Path
    private lateinit var userCountriesConditionPath: Path
    override val onDemandFeatureName: String = "dynamic"
    override val installTimeFeatureName: String = "installtimefeature"
    override val taskName: String = ":app:writeGloballyDynamicConfigurationSourceFilesFor${VARIANT.capitalize()}"

    override fun beforeEach() {
        configurationPath = appModuleProjectDirPath.resolve("build")
            .resolve("generated")
            .resolve("source")
            .resolve("globallydynamic")
            .resolve(VARIANT)
            .resolve("com")
            .resolve("jeppeman")
            .resolve("globallydynamic")
            .resolve("generated")

        globallyDynamicBuildConfigPath = configurationPath.resolve("GloballyDynamicBuildConfig.java")
        deviceFeatureConditionPath = configurationPath.resolve("DeviceFeatureCondition.java")
        moduleConditionsPath = configurationPath.resolve("ModuleConditions.java")
        userCountriesConditionPath = configurationPath.resolve("UserCountriesCondition.java")

        appModuleAndroidManifestFilePath.toFile().writeText(
            """
                <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                    <application>
                        <activity android:exported="true" android:name="$MAIN_ACTIVITY_NAME" >
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
                    id 'com.jeppeman.globallydynamic'
                }
                
                android {
                    namespace '$BASE_PACKAGE_NAME'
                    
                    defaultConfig {
                        compileSdkVersion 32
                        minSdkVersion 16
                        targetSdkVersion 32
                        versionCode $VERSION_CODE
                    }
                    
                    buildTypes {
                        $VARIANT {
                            matchingFallbacks = ['debug']
                        }
                    }
                    
                    globallyDynamicServers {
                        server {
                            serverUrl = "$SERVER_URL"
                            username = "$USERNAME"
                            password = "$PASSWORD"
                            downloadConnectTimeout = $DOWNLOAD_CONNECT_TIMEOUT
                            downloadReadTimeout = $DOWNLOAD_READ_TIMEOUT
                            throttleDownloadBy = $THROTTLE_DOWNLOAD_BY
                            applyToBuildVariants '$VARIANT'
                        }
                    }
                    
                    dynamicFeatures = [':$onDemandFeatureName', ':$installTimeFeatureName']
                }
                
                dependencies {
                    implementation 'com.jeppeman.globallydynamic.android:selfhosted:${ANDROID_LIB_VERSION}'
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
                    xmlns:dist="http://schemas.android.com/apk/distribution">
                    
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
            globallyDynamicBuildConfigPath.toFile()
        )
        val stringWriter = StringWriter()
        val writer = PrintWriter(stringWriter)
        val task = compiler.getTask(writer, fileManager, null, null, null, files)
        val success = task.call()
        return if (success) {
            val classLoader = CompiledClassLoader(fileManager.generatedOutputFiles)
            classLoader.loadClass(GLOBALLY_DYNAMIC_BUILD_CONFIG_CLASS).newInstance()
        } else {
            throw Exception(stringWriter.toString())
        }
    }

    @Test
    fun whenConfigurationIsProvidedThroughDsl_task_shouldGenerateFilesAccordingly() {
        val result = runTask()
        val globallyDynamicBuildConfig = compileGeneratedFiles()
        val installTimeFeatures = globallyDynamicBuildConfig.invokeMethod<Map<String, Any>>("getInstallTimeFeatures")
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
        assertThat(globallyDynamicBuildConfig.invokeMethod<String>("getMainActivityFullyQualifiedName")).isEqualTo(MAIN_ACTIVITY_NAME)
        assertThat(globallyDynamicBuildConfig.invokeMethod<String>("getServerUrl")).isEqualTo(SERVER_URL)
        assertThat(globallyDynamicBuildConfig.invokeMethod<String>("getVariantName")).isEqualTo(VARIANT)
        assertThat(globallyDynamicBuildConfig.invokeMethod<Long>("getDownloadConnectTimeout")).isEqualTo(DOWNLOAD_CONNECT_TIMEOUT)
        assertThat(globallyDynamicBuildConfig.invokeMethod<Long>("getDownloadReadTimeout")).isEqualTo(DOWNLOAD_READ_TIMEOUT)
        assertThat(globallyDynamicBuildConfig.invokeMethod<Long>("getThrottleDownloadBy")).isEqualTo(THROTTLE_DOWNLOAD_BY)
        assertThat(globallyDynamicBuildConfig.invokeMethod<Int>("getVersionCode")).isEqualTo(VERSION_CODE)
        assertThat(globallyDynamicBuildConfig.invokeMethod<Array<String>>("getOnDemandFeatures")).isEqualTo(arrayOf(onDemandFeatureName))
    }

    @Test
    fun whenConfigurationIsProvidedThroughArguments_task_shouldGenerateFilesAccordingly() {
        val serverUrlArgument = "http://program.argument"
        val throttleByArgument = 1000
        val downloadConnectTimeoutArgument = 5000
        val downloadReadTimeoutArgument = 10000

        val result = runTask(
            "-PgloballyDynamicServer.serverUrl=$serverUrlArgument",
            "-PgloballyDynamicServer.throttleDownloadBy=$throttleByArgument",
            "-PgloballyDynamicServer.downloadConnectTimeout=$downloadConnectTimeoutArgument",
            "-PgloballyDynamicServer.downloadReadTimeout=$downloadReadTimeoutArgument"
        )
        val globallyDynamicBuildConfig = compileGeneratedFiles()

        assertThat(result.task(taskName)?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(globallyDynamicBuildConfig.invokeMethod<String>("getServerUrl")).isEqualTo(serverUrlArgument)
        assertThat(globallyDynamicBuildConfig.invokeMethod<Long>("getDownloadConnectTimeout")).isEqualTo(5000L)
        assertThat(globallyDynamicBuildConfig.invokeMethod<Long>("getDownloadReadTimeout")).isEqualTo(10000L)
        assertThat(globallyDynamicBuildConfig.invokeMethod<Long>("getThrottleDownloadBy")).isEqualTo(throttleByArgument)
    }

    @Test
    fun whenConfigurationIsProvidedThroughFile_task_shouldGenerateFilesAccordingly() {
        appModuleBuildFilePath.toFile().writeText(
            """
                plugins {
                    id 'com.android.application'
                    id 'com.jeppeman.globallydynamic'
                }
                
                android {
                    namespace '$BASE_PACKAGE_NAME'
                    
                    defaultConfig {
                        compileSdkVersion 32
                        minSdkVersion 16
                        targetSdkVersion 32
                        versionCode $VERSION_CODE
                    }
                    
                    buildTypes {
                        $VARIANT {
                            matchingFallbacks = ['debug']
                        }
                    }
                    
                    globallyDynamicServers {
                        server {
                            throttleDownloadBy = $THROTTLE_DOWNLOAD_BY
                            applyToBuildVariants '$VARIANT'
                        }
                    }
                    
                    dynamicFeatures = [':$onDemandFeatureName', ':$installTimeFeatureName']
                }
                
                dependencies {
                    implementation 'com.jeppeman.globallydynamic.android:selfhosted:${ANDROID_LIB_VERSION}'
                    implementation 'androidx.appcompat:appcompat:1.4.2'
                }
            """.trimIndent()
        )
        val globallyDynamicServerInfo = GloballyDynamicServerInfoDto(
            serverUrl = "http://server.url",
            username = "username",
            password = "password"
        )
        appModuleProjectDirPath.resolve("build")
            .resolve("globallydynamic")
            .apply { toFile().mkdirs() }
            .resolve("server_info.json")
            .toFile()
            .writeText(Gson().toJson(globallyDynamicServerInfo))

        val result = runTask()
        val globallyDynamicBuildConfig = compileGeneratedFiles()

        assertThat(result.task(taskName)?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(globallyDynamicBuildConfig.invokeMethod<String>("getServerUrl")).isEqualTo(globallyDynamicServerInfo.serverUrl)
        assertThat(globallyDynamicBuildConfig.invokeMethod<Long>("getDownloadConnectTimeout")).isEqualTo(15 * 1000L)
        assertThat(globallyDynamicBuildConfig.invokeMethod<Long>("getDownloadReadTimeout")).isEqualTo(2 * 60 * 1000L)
        assertThat(globallyDynamicBuildConfig.invokeMethod<Long>("getThrottleDownloadBy")).isEqualTo(THROTTLE_DOWNLOAD_BY)
    }

    companion object {
        private const val CLASS_PREFIX = "com.jeppeman.globallydynamic.generated"
        private const val GLOBALLY_DYNAMIC_BUILD_CONFIG_CLASS = "$CLASS_PREFIX.GloballyDynamicBuildConfig"
        private const val MAIN_ACTIVITY_NAME = "$BASE_PACKAGE_NAME.GloballyDynamicActivity"
        private const val SERVER_URL = "http://globallydynamic.io"
        private const val USERNAME = "username"
        private const val PASSWORD = "password"
        private const val THROTTLE_DOWNLOAD_BY = 5000
        private const val DOWNLOAD_CONNECT_TIMEOUT = 30 * 1000L
        private const val DOWNLOAD_READ_TIMEOUT = 4 * 60 * 1000L
        private const val VARIANT = "instrumentation"
        private const val VERSION_CODE = 5
        private const val INSTALL_TIME_MIN_SDK = 16
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