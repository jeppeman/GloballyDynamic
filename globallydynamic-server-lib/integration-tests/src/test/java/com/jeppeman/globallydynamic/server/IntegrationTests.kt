package com.jeppeman.globallydynamic.server

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.jeppeman.globallydynamic.server.extensions.deleteCompletely
import com.jeppeman.globallydynamic.server.extensions.toBase64
import com.jeppeman.globallydynamic.server.extensions.unzip
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
    private lateinit var configuration: GloballyDynamicServer.Configuration
    private lateinit var globallyDynamicServer: GloballyDynamicServer
    private val httpClient: HttpClient = HttpClients.createDefault()
    private val auth get() = "${configuration.username}:${configuration.password}".toBase64()

    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun setUp() {
        configuration = GloballyDynamicServer.Configuration.builder()
            .setPort(0)
            .setUsername("username")
            .setPassword("password")
            .setStorageBackend(
                LocalStorageBackend.builder()
                    .setBaseStoragePath(tempDir)
                    .build()
            )
            .build()
        globallyDynamicServer = GloballyDynamicServer(configuration).apply {
            start()
        }
    }

    @AfterEach
    fun tearDown() {
        tempDir.deleteCompletely()
        globallyDynamicServer.stop()
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
            .setUri("${globallyDynamicServer.address}/upload")
            .addHeader("Authorization", "Basic $auth")
            .setEntity(multipartEntity)
            .build()

        httpClient.execute(request)
    }

    private fun downloadApks(version: Int, applicationId: String, variantName: String, signature: String): Path {
        val languages = URLEncoder.encode(listOf("it", "de", "ko").joinToString(","), "UTF-8")
        val features = URLEncoder.encode(listOf("ondemandfeature").joinToString(","), "UTF-8")
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
            .setUri("${globallyDynamicServer.address}/download" +
                "?version=$version" +
                "&variant=$variantName" +
                "&application-id=$applicationId" +
                "&signature=$signature" +
                "&languages=$languages" +
                "&features=$features")
            .setEntity(StringEntity(deviceSpec, ContentType.APPLICATION_JSON))
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
        val signature = "signature"

        uploadBundle(version, applicationId, variant)

        val downloadedApks = downloadApks(version, applicationId, variant, signature)

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