package com.jeppeman.locallydynamic.server

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.jeppeman.locallydynamic.server.extensions.deleteCompletely
import com.jeppeman.locallydynamic.server.extensions.readString
import com.jeppeman.locallydynamic.server.extensions.toBase64
import com.jeppeman.locallydynamic.server.extensions.unzip
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.RequestBuilder
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.entity.mime.HttpMultipartMode
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.StringBody
import org.apache.http.impl.client.HttpClients
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import java.net.URLEncoder
import java.nio.file.Path
import java.nio.file.Paths

@RunWith(JUnitPlatform::class)
class IntegrationTests {
    private lateinit var configuration: LocallyDynamicServer.Configuration
    private lateinit var locallyDynamicServer: LocallyDynamicServer
    private val httpClient: HttpClient = HttpClients.createDefault()
    private val auth get() = "${configuration.username}:${configuration.password}".toBase64()

    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun setUp() {
        configuration = LocallyDynamicServer.Configuration.builder()
            .setPort(0)
            .setUsername("username")
            .setPassword("password")
            .setStorageBackend(
                LocalStorageBackend.builder()
                    .setBaseStoragePath(tempDir)
                    .build()
            )
            .build()
        locallyDynamicServer = LocallyDynamicServer(configuration).apply {
            start()
        }
    }

    @AfterEach
    fun tearDown() {
        tempDir.deleteCompletely()
        locallyDynamicServer.stop()
    }

    private fun registerDevice(): String {
        val deviceSpec = """{
                "supportedAbis": ["x86"],
                "supportedLocales": ["en"],
                "deviceFeatures": ["android.hardware.camera"],
                "glExtensions": ["GL_IMAGE"],
                "screenDensity": 420,
                "sdkVersion": 23
            }
        """.trimMargin()
        val request = RequestBuilder.post()
            .setUri("${locallyDynamicServer.address}/register")
            .addHeader("Authorization", "Basic $auth")
            .setEntity(StringEntity(
                deviceSpec,
                ContentType.APPLICATION_JSON
            ))
            .build()

        val response = httpClient.execute(request)

        return response.entity.content.readString()
    }

    private fun uploadBundle(version: Int, applicationId: String, variantName: String) {
        val testResPath = Paths.get("src", "test", "resources")
        val bundleFile = Paths.get(testResPath.toString(), "testbundle.aab").toFile()
        val keyStoreFile = Paths.get(testResPath.toString(), "test.keystore").toFile()
        val json = mapOf(
            "keyAlias" to "androiddebugkey",
            "storePassword" to "android",
            "keyPassword" to "android"
        )
        val multipartEntity = MultipartEntityBuilder.create()
            .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
            .addBinaryBody("bundle", bundleFile, ContentType.APPLICATION_OCTET_STREAM, bundleFile.name)
            .addBinaryBody("keystore", keyStoreFile, ContentType.APPLICATION_OCTET_STREAM, keyStoreFile.name)
            .addTextBody("version", version.toString())
            .addTextBody("application-id", applicationId)
            .addTextBody("variant", variantName)
            .addPart("signing-config", StringBody(Gson().toJson(json), ContentType.APPLICATION_JSON))
            .build()

        val request = RequestBuilder.post()
            .setUri("${locallyDynamicServer.address}/upload")
            .addHeader("Authorization", "Basic $auth")
            .setEntity(multipartEntity)
            .build()

        httpClient.execute(request)
    }

    private fun downloadApks(deviceId: String, version: Int, applicationId: String, variantName: String): Path {
        val languages = URLEncoder.encode(listOf("it", "de", "ko").joinToString(","), "UTF-8")
        val features = URLEncoder.encode(listOf("ondemandfeature").joinToString(","), "UTF-8")
        val request = RequestBuilder.get()
            .setUri("${locallyDynamicServer.address}/download" +
                "?device-id=$deviceId" +
                "&version=$version" +
                "&variant=$variantName" +
                "&application-id=$applicationId" +
                "&languages=$languages" +
                "&features=$features")
            .addHeader("Authorization", "Basic $auth")
            .build()

        val response = httpClient.execute(request)

        val ret = tempDir.resolve("downloaded_apks.zip")
        response.entity.content.copyTo(ret.toFile().outputStream())

        return ret
    }

    @Test
    fun runRegisterUploadDownloadFlow() {
        val applicationId = "application"
        val version = 23
        val variant = "variant"
        val unzipDir = tempDir.resolve("unzipped_apks")

        val deviceId = registerDevice()

        uploadBundle(version, applicationId, variant)

        val downloadedApks = downloadApks(deviceId, version, applicationId, variant)

        downloadedApks.toFile().unzip(unzipDir.toString())

        val unzippedApks = unzipDir.toFile().listFiles()!!.toList().map { it.name }

        assertThat(unzippedApks).contains("base-it.apk")
        assertThat(unzippedApks).contains("base-ko.apk")
        assertThat(unzippedApks).contains("base-de.apk")
        assertThat(unzippedApks).contains("ondemandfeature-master.apk")
        assertThat(unzippedApks).contains("ondemandfeature-xxhdpi.apk")
        assertThat(unzippedApks).contains("ondemandfeature-it.apk")
        assertThat(unzippedApks).contains("ondemandfeature-de.apk")
        assertThat(unzippedApks).contains("ondemandfeature-ko.apk")
    }
}