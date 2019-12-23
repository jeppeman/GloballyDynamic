package com.jeppeman.locallydynamic.gradle

import com.android.build.gradle.api.ApplicationVariant
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.jeppeman.locallydynamic.gradle.extensions.getTaskName
import com.jeppeman.locallydynamic.gradle.extensions.toBase64
import org.apache.http.HttpException
import org.apache.http.HttpStatus
import org.apache.http.client.methods.RequestBuilder
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.HttpMultipartMode
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.StringBody
import org.apache.http.impl.client.HttpClients
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import java.io.File
import java.net.URI

/**
 * A task which uploads the bundle generated from package<Flavor><BuildType>Bundle to the locally dynamic
 * server. Used as follows: ./gradlew uploadLocallyDynamicBundle<Flavor><BuildType> e.g ./gradlew uploadLocallyDynamicBundleFreeDebug
 *
 * There is no need to call this task manually as it is automatically invoked after package<Flavor><BuildType>Bundle
 */
open class UploadBundleTask : DefaultTask() {
    private val gson: Gson by lazy { GsonBuilder().create() }

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    lateinit var bundleDir: File
        private set

    @get:Input
    lateinit var serverInfo: LocallyDynamicServerInfo
        private set

    @get:Input
    lateinit var applicationId: String
        private set

    @get:Input
    lateinit var signingConfig: File
        private set

    @get:Input
    var version: Int = -1
        private set

    @get:Input
    lateinit var variantName: String
        private set

    // AGP 3.5 to 3.6 compatibility
    private fun JsonObject.getPropertyCompat(propName: String) =
        get(propName) ?: get("m${propName.capitalize()}")

    @TaskAction
    fun doTaskAction() {
        try {
            val serverInfo = requireNotNull(this.serverInfo) {
                "No server info for locallydynamic found, specify it through properties, the extension or by" +
                    " running the server in Android Studio"
            }
            require(serverInfo.serverUrl?.isNotBlank() == true) {
                "No server url for locallydynamic found, specify it through properties, the extension or by" +
                    " running the server in Android Studio"
            }
            val httpClient = HttpClients.createDefault()

            val uri = URI.create(serverInfo.serverUrl!!).run {
                URI(scheme, userInfo, host, port, "/upload", query, fragment)
            }

            val bundle = bundleDir.listFiles { file -> file.name.contains(".aab") }!!.first()
            val signingConfigJson = signingConfig.readText()
            val signingConfig = gson.fromJson(signingConfigJson, JsonObject::class.java)
            val sanitizedConfig = JsonObject().apply {
                add("storeFile", signingConfig.getPropertyCompat("storeFile"))
                add("storePassword", signingConfig.getPropertyCompat("storePassword"))
                add("keyAlias", signingConfig.getPropertyCompat("keyAlias"))
                add("keyPassword", signingConfig.getPropertyCompat("keyPassword"))
            }
            val signingConfigPart = StringBody(sanitizedConfig.toString(), ContentType.APPLICATION_JSON)

            val entity = MultipartEntityBuilder.create()
                .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                .addTextBody("version", version.toString())
                .addTextBody("application-id", applicationId)
                .addTextBody("variant", variantName)
                .addPart("signing-config", signingConfigPart)
                .addBinaryBody(
                    "bundle",
                    bundle,
                    ContentType.APPLICATION_OCTET_STREAM,
                    bundle.name
                )
                .addBinaryBody(
                    "keystore",
                    File(sanitizedConfig.get("storeFile").asString),
                    ContentType.APPLICATION_OCTET_STREAM,
                    "keystore"
                )
                .build()

            val requestBuilder = RequestBuilder
                .post(uri)
                .setEntity(entity)

            if (serverInfo.username != null || serverInfo.password != null) {
                val authorization = "${serverInfo.username}:${serverInfo.password}".toBase64()
                requestBuilder.addHeader("Authorization", "Basic $authorization")
            }

            val request = requestBuilder.build()

            val resp = httpClient.execute(request)

            if (resp.statusLine.statusCode != HttpStatus.SC_OK) {
                throw HttpException(resp.statusLine.toString())
            }
        } catch (exception: Exception) {
            if (serverInfo.serverUrl != null) {
                System.err.println("Failed to upload bundle, make sure the server is running and is reachable at " +
                    "${serverInfo.serverUrl}, reason:\n${exception.message}")
            }

            throw exception
        }
    }

    class CreationAction(
        applicationVariant: ApplicationVariant,
        private val extension: LocallyDynamicExtension
    ) : VariantTaskAction<UploadBundleTask>(applicationVariant) {
        override val name: String
            get() = applicationVariant.getTaskName("uploadLocallyDynamicBundle")

        override fun execute(task: UploadBundleTask) {
            task.variantName = applicationVariant.name
            task.applicationId = applicationVariant.applicationId
            task.version = applicationVariant.versionCode
            task.serverInfo = task.project.resolveServerInfo(extension)
            task.bundleDir = task.project.buildDir
                .toPath()
                .resolve("intermediates")
                .resolve("intermediary_bundle")
                .resolve(applicationVariant.name)
                .toFile()
            task.signingConfig = task.project.buildDir
                .toPath()
                .resolve("intermediates")
                .resolve("signing_config")
                .resolve(applicationVariant.name)
                .resolve("out")
                .resolve("signing-config.json")
                .toFile()
        }
    }
}