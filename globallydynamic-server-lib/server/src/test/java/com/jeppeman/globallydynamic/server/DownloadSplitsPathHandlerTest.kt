package com.jeppeman.globallydynamic.server

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.eclipse.jetty.http.HttpStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.nio.file.Path
import javax.servlet.ServletOutputStream
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@RunWith(JUnitPlatform::class)
@ExtendWith(MockitoExtension::class)
class DownloadSplitsPathHandlerTest {
    @Mock
    private lateinit var mockBundleManager: BundleManager
    @Mock
    private lateinit var mockLogger: Logger
    @Mock
    private lateinit var mockRequest: HttpServletRequest
    @Mock
    private lateinit var mockResponse: HttpServletResponse
    private lateinit var downloadSplitsPathHandler: DownloadSplitsPathHandler

    @BeforeEach
    fun setUp() {
        downloadSplitsPathHandler = DownloadSplitsPathHandler(
            bundleManager = mockBundleManager,
            logger = mockLogger,
            validateSignature = false,
            gson = Gson()
        )
    }

    private fun mockCompleteRequest() {
        val deviceSpec = """{
                "supportedAbis": ["x86"],
                "supportedLocales": ["en"],
                "deviceFeatures": ["android.hardware.camera"],
                "glExtensions": ["GL_IMAGE"],
                "screenDensity": 420,
                "sdkVersion": 23
            }
        """.trimMargin()
        whenever(mockRequest.inputStream).thenReturn(FakeInputStream(deviceSpec))
        whenever(mockRequest.parameterMap).thenReturn(mapOf(
            "application-id" to arrayOf("applicationId"),
            "signature" to arrayOf("signature"),
            "version" to arrayOf("1"),
            "variant" to arrayOf("variant"),
            "features" to arrayOf("feature"),
            "languages" to arrayOf(""),
            "throttle" to arrayOf("2000")
        ))
    }

    @Test
    fun whenRequestLacksBody_handle_shouldThrowWith400() {
        mockCompleteRequest()
        whenever(mockRequest.inputStream).thenReturn(null)
        val executable = { downloadSplitsPathHandler.handle(mockRequest, mockResponse) }

        val thrown = assertThrows<HttpException>(executable)
        assertThat(thrown.code).isEqualTo(HttpStatus.BAD_REQUEST_400)
        assertThat(thrown.message).contains("Invalid body")
    }

    @Test
    fun whenRequestLacksApplicationId_handle_shouldThrowWith400() {
        whenever(mockRequest.parameterMap).thenReturn(mapOf("device-id" to arrayOf("")))

        val executable = { downloadSplitsPathHandler.handle(mockRequest, mockResponse) }

        val thrown = assertThrows<HttpException>(executable)
        assertThat(thrown.code).isEqualTo(HttpStatus.BAD_REQUEST_400)
        assertThat(thrown.message).contains("application-id")
    }

    @Test
    fun whenRequestLacksVersion_handle_shouldThrowWith400() {
        whenever(mockRequest.parameterMap).thenReturn(mapOf(
            "device-id" to arrayOf(""),
            "application-id" to arrayOf("")
        ))

        val executable = { downloadSplitsPathHandler.handle(mockRequest, mockResponse) }

        val thrown = assertThrows<HttpException>(executable)
        assertThat(thrown.code).isEqualTo(HttpStatus.BAD_REQUEST_400)
        assertThat(thrown.message).contains("version")
    }

    @Test
    fun whenRequestLacksVariant_handle_shouldThrowWith400() {
        whenever(mockRequest.parameterMap).thenReturn(mapOf(
            "device-id" to arrayOf(""),
            "application-id" to arrayOf(""),
            "version" to arrayOf("")
        ))

        val executable = { downloadSplitsPathHandler.handle(mockRequest, mockResponse) }

        val thrown = assertThrows<HttpException>(executable)
        assertThat(thrown.code).isEqualTo(HttpStatus.BAD_REQUEST_400)
        assertThat(thrown.message).contains("variant")
    }

    @Test
    fun whenRequestLacksFeaturesAndLanguages_handle_shouldThrowWith400() {
        whenever(mockRequest.parameterMap).thenReturn(mapOf(
            "device-id" to arrayOf(""),
            "application-id" to arrayOf(""),
            "version" to arrayOf(""),
            "variant" to arrayOf("")
        ))

        val executable = { downloadSplitsPathHandler.handle(mockRequest, mockResponse) }

        val thrown = assertThrows<HttpException>(executable)
        assertThat(thrown.code).isEqualTo(HttpStatus.BAD_REQUEST_400)
    }

    @Test
    fun whenVersionIsInteger_handle_shouldThrowWith400() {
        mockCompleteRequest()
        whenever(mockRequest.parameterMap).thenReturn(mapOf(
            "device-id" to arrayOf("deviceId"),
            "application-id" to arrayOf("applicationId"),
            "signature" to arrayOf("signature"),
            "version" to arrayOf("fff"),
            "variant" to arrayOf("variant"),
            "features" to arrayOf("feature"),
            "languages" to arrayOf("")
        ))

        val executable = { downloadSplitsPathHandler.handle(mockRequest, mockResponse) }

        val thrown = assertThrows<HttpException>(executable)
        assertThat(thrown.code).isEqualTo(HttpStatus.BAD_REQUEST_400)
        assertThat(thrown.message).contains("Expected version to be an integer")
    }

    @Test
    fun whenGenerateCompressedSplitsFails_handle_shouldThrowWith400() {
        mockCompleteRequest()
        whenever(mockBundleManager.generateCompressedSplits(any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(BundleManager.Result.Error.MissingFeaturesAndLanguages)

        val executable = { downloadSplitsPathHandler.handle(mockRequest, mockResponse) }

        val thrown = assertThrows<HttpException>(executable)
        assertThat(thrown.code).isEqualTo(HttpStatus.BAD_REQUEST_400)
    }

    @Test
    fun whenBundleExists_handle_shouldSendCompressedSplits(@TempDir tempDir: Path) {
        val tempFile = tempDir.resolve("temp.txt").toFile()
        val content = ByteArray(4096)
        content.fill(Byte.MAX_VALUE)
        tempFile.writeBytes(content)
        mockCompleteRequest()
        whenever(mockBundleManager.generateCompressedSplits(any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(BundleManager.Result.Success(tempFile.toPath()))
        val writtenBytes = mutableListOf<Byte>()
        val outputStream = mock<ServletOutputStream>()
        whenever(outputStream.write(any(), any(), any())).thenAnswer { invocation ->
            val bytes = invocation.getArgument<ByteArray>(0)
            val from = invocation.getArgument<Int>(1)
            val length = invocation.getArgument<Int>(2)
            writtenBytes.addAll(bytes.toList().subList(from, from + length))
        }
        whenever(mockResponse.outputStream).thenReturn(outputStream)
        val startTime = System.currentTimeMillis()

        downloadSplitsPathHandler.handle(mockRequest, mockResponse)

        assertThat(writtenBytes).isEqualTo(content.toList())
        assertThat(System.currentTimeMillis() - startTime).isAtLeast(2000)
        verify(mockResponse).contentType = "application/zip"
        verify(mockResponse).setHeader("Content-Disposition", "attachment; filename=splits.zip")
        verify(mockResponse).setContentLength(content.size)
    }
}