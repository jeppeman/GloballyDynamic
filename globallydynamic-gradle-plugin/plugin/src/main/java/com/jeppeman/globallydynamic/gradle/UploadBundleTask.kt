package com.jeppeman.globallydynamic.gradle

import com.android.build.gradle.api.ApplicationVariant
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.jeppeman.globallydynamic.gradle.extensions.*
import com.jeppeman.globallydynamic.gradle.extensions.stackTraceToString
import com.jeppeman.globallydynamic.gradle.extensions.toBase64
import org.apache.http.HttpException
import org.apache.http.HttpStatus
import org.apache.http.client.methods.RequestBuilder
import org.apache.http.client.utils.URIBuilder
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.HttpMultipartMode
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.StringBody
import org.apache.http.impl.client.HttpClients
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import java.io.File

/**
 * A task which uploads the bundle generated from package<Flavor><BuildType>Bundle to the globally dynamic
 * server. Used as follows: ./gradlew uploadGloballyDynamicBundle<Flavor><BuildType> e.g ./gradlew uploadGloballyDynamicBundleFreeDebug
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
    internal lateinit var serverInfo: GloballyDynamicServerInfoDto
        private set

    @get:Input
    lateinit var applicationId: String
        private set

    @get:InputFile
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
                "No server info for globallydynamic found, specify it through properties, the extension or by" +
                    " running the server in Android Studio"
            }
            require(serverInfo.serverUrl?.isNotBlank() == true) {
                "No server url for globallydynamic found, specify it through properties, the extension or by" +
                    " running the server in Android Studio"
            }
            val httpClient = HttpClients.createDefault()

            val uri = URIPathBuilder(serverInfo.serverUrl!!).addPathSegment("upload").build()

            val bundle = bundleDir.listFiles { file -> file.name.contains(".aab") }!!.first()
            val signingConfigJson = signingConfig.readText()
            if (signingConfigJson.isBlank() || signingConfigJson == "null") {
                throw IllegalStateException("""No signing config found, make sure that you have added it to your
                    | build variant $variantName
                """.trimMargin())
            }

            val signingConfigJsonObject = gson.fromJson(signingConfigJson, JsonObject::class.java)
            val sanitizedConfig = JsonObject().apply {
                add("storeFile", signingConfigJsonObject.getPropertyCompat("storeFile"))
                add("storePassword", signingConfigJsonObject.getPropertyCompat("storePassword"))
                add("keyAlias", signingConfigJsonObject.getPropertyCompat("keyAlias"))
                add("keyPassword", signingConfigJsonObject.getPropertyCompat("keyPassword"))
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
                val body = resp.entity.content.use { it.readBytes().toString(Charsets.UTF_8) }
                throw HttpException("${resp.statusLine}, $body")
            }
        } catch (exception: Exception) {
            System.err.println("Failed to upload bundle, ${exception.stackTraceToString()}")
            throw exception
        }
    }

    class CreationAction(
        applicationVariant: ApplicationVariant,
        private val extension: GloballyDynamicServer
    ) : VariantTaskAction<UploadBundleTask>(applicationVariant) {
        override val name: String
            get() = applicationVariant.getTaskName("uploadGloballyDynamicBundle")

        override fun execute(task: UploadBundleTask) {
            task.variantName = applicationVariant.name
            task.applicationId = applicationVariant.applicationId
            task.version = applicationVariant.versionCode
            task.serverInfo = task.project.resolveServerInfo(extension)
            task.bundleDir = task.project.intermediaryBundleDir(applicationVariant)
            task.signingConfig = task.project.intermediarySigningConfig(applicationVariant)
        }
    }
}

private class URIPathBuilder(uri: String) : URIBuilder(uri) {
    private fun appendSegmentToPath(path: String?, segment: String): String? {
        return if (path == null) {
            if (segment.startsWith("/")) segment else "/$segment"
        } else if (path.isBlank() || path[path.length - 1] == '/' || segment.startsWith("/")) {
            path + segment
        } else "$path/$segment"
    }

    fun addPathSegment(segment: String): URIBuilder = setPath(appendSegmentToPath(path, segment))
}