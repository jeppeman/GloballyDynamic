package com.jeppeman.globallydynamic.server

import com.google.common.truth.Truth.assertThat
import com.jeppeman.globallydynamic.server.*
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.eclipse.jetty.http.HttpStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.Part

@RunWith(JUnitPlatform::class)
@ExtendWith(MockitoExtension::class)
class UploadBundlePathHandlerTest {
    @Mock
    private lateinit var mockBundleManager: BundleManager
    @Mock
    private lateinit var mockLogger: Logger
    @Mock
    private lateinit var mockRequest: HttpServletRequest
    @Mock
    private lateinit var mockResponse: HttpServletResponse
    private lateinit var uploadBundlePathHandler: UploadBundlePathHandler

    @BeforeEach
    fun setUp() {
        uploadBundlePathHandler = UploadBundlePathHandler(
            bundleManager = mockBundleManager,
            logger = mockLogger
        )
    }

    @Test
    fun whenContentTypeIsNotMultipart_handle_shouldThrowWith400() {
        val executable = { uploadBundlePathHandler.handle(mockRequest, mockResponse) }

        val thrown = assertThrows<HttpException>(executable)
        assertThat(thrown.code).isEqualTo(HttpStatus.BAD_REQUEST_400)
    }

    @Test
    fun whenBodyIsMissingBundlePart_handle_shouldThrowWith400() {
        whenever(mockRequest.contentType).thenReturn(UploadBundlePathHandler.CONTENT_TYPE_MULTIPART_FORM_DATA)

        val executable = { uploadBundlePathHandler.handle(mockRequest, mockResponse) }

        val thrown = assertThrows<HttpException>(executable)
        assertThat(thrown.code).isEqualTo(HttpStatus.BAD_REQUEST_400)
        assertThat(thrown.message).contains("bundle")
    }

    @Test
    fun whenBodyIsMissingVersionPart_handle_shouldThrowWith400() {
        val mockBundlePart = mock<Part> { on { name } doReturn "bundle" }
        whenever(mockRequest.contentType).thenReturn(UploadBundlePathHandler.CONTENT_TYPE_MULTIPART_FORM_DATA)
        whenever(mockRequest.parts).thenReturn(listOf(mockBundlePart))

        val executable = { uploadBundlePathHandler.handle(mockRequest, mockResponse) }

        val thrown = assertThrows<HttpException>(executable)
        assertThat(thrown.code).isEqualTo(HttpStatus.BAD_REQUEST_400)
        assertThat(thrown.message).contains("version")
    }

    @Test
    fun whenBodyIsMissingApplicationIdPart_handle_shouldThrowWith400() {
        val mockBundlePart = mock<Part> { on { name } doReturn "bundle" }
        val mockVersionPart = mock<Part> { on { name } doReturn "version" }
        whenever(mockRequest.contentType).thenReturn(UploadBundlePathHandler.CONTENT_TYPE_MULTIPART_FORM_DATA)
        whenever(mockRequest.parts).thenReturn(listOf(mockBundlePart, mockVersionPart))

        val executable = { uploadBundlePathHandler.handle(mockRequest, mockResponse) }

        val thrown = assertThrows<HttpException>(executable)
        assertThat(thrown.code).isEqualTo(HttpStatus.BAD_REQUEST_400)
        assertThat(thrown.message).contains("application-id")
    }

    @Test
    fun whenBodyIsMissingVariantPart_handle_shouldThrowWith400() {
        val mockBundlePart = mock<Part> { on { name } doReturn "bundle" }
        val mockVersionPart = mock<Part> { on { name } doReturn "version" }
        val mockApplicationIdPart = mock<Part> { on { name } doReturn "application-id" }
        whenever(mockRequest.contentType).thenReturn(UploadBundlePathHandler.CONTENT_TYPE_MULTIPART_FORM_DATA)
        whenever(mockRequest.parts).thenReturn(listOf(mockBundlePart, mockVersionPart, mockApplicationIdPart))

        val executable = { uploadBundlePathHandler.handle(mockRequest, mockResponse) }

        val thrown = assertThrows<HttpException>(executable)
        assertThat(thrown.code).isEqualTo(HttpStatus.BAD_REQUEST_400)
        assertThat(thrown.message).contains("variant")
    }

    @Test
    fun whenVersionPartIsNotInteger_handle_shouldThrowWith400() {
        val mockBundlePart = mock<Part> { on { name } doReturn "bundle" }
        val mockApplicationIdPart = mock<Part> {
            on { name } doReturn "application-id"
            on { inputStream } doReturn FakeInputStream("application-id")
        }
        val mockVariantPart = mock<Part> {
            on { name } doReturn "variant"
            on { inputStream } doReturn FakeInputStream("variant")
        }
        val mockVersionPart = mock<Part> {
            on { name } doReturn "version"
            on { inputStream } doReturn FakeInputStream("fff")
        }
        val mockSigningConfigPart = mock<Part> {
            on { name } doReturn "signing-config"
            on { inputStream } doReturn FakeInputStream("signingConfig")
        }
        val mockKeystorePart = mock<Part> {
            on { name } doReturn "keystore"
        }
        whenever(mockRequest.contentType).thenReturn(UploadBundlePathHandler.CONTENT_TYPE_MULTIPART_FORM_DATA)
        whenever(mockRequest.parts).thenReturn(listOf(
            mockBundlePart,
            mockVersionPart,
            mockApplicationIdPart,
            mockVariantPart,
            mockSigningConfigPart,
            mockKeystorePart
        ))

        val executable = { uploadBundlePathHandler.handle(mockRequest, mockResponse) }

        val thrown = assertThrows<HttpException>(executable)
        assertThat(thrown.code).isEqualTo(HttpStatus.BAD_REQUEST_400)
        assertThat(thrown.message).contains("Expected version to be an integer")
    }

    @Test
    fun whenSigningConfigPartIsMissing_handle_shouldThrowWith400() {
        val mockBundlePart = mock<Part> { on { name } doReturn "bundle" }
        val mockApplicationIdPart = mock<Part> {
            on { name } doReturn "application-id"
        }
        val mockVariantPart = mock<Part> {
            on { name } doReturn "variant"
        }
        val mockVersionPart = mock<Part> {
            on { name } doReturn "version"
        }
        whenever(mockRequest.contentType).thenReturn(UploadBundlePathHandler.CONTENT_TYPE_MULTIPART_FORM_DATA)
        whenever(mockRequest.parts).thenReturn(listOf(mockBundlePart, mockVersionPart, mockApplicationIdPart, mockVariantPart))

        val executable = { uploadBundlePathHandler.handle(mockRequest, mockResponse) }

        val thrown = assertThrows<HttpException>(executable)
        assertThat(thrown.code).isEqualTo(HttpStatus.BAD_REQUEST_400)
        assertThat(thrown.message).contains("signing-config")
    }

    @Test
    fun whenKeystorePartIsMissing_handle_shouldThrowWith400() {
        val mockBundlePart = mock<Part> { on { name } doReturn "bundle" }
        val mockApplicationIdPart = mock<Part> {
            on { name } doReturn "application-id"
        }
        val mockVariantPart = mock<Part> {
            on { name } doReturn "variant"
        }
        val mockVersionPart = mock<Part> {
            on { name } doReturn "version"
        }
        val mockSigningConfigPart = mock<Part> {
            on { name } doReturn "signing-config"
        }
        whenever(mockRequest.contentType).thenReturn(UploadBundlePathHandler.CONTENT_TYPE_MULTIPART_FORM_DATA)
        whenever(mockRequest.parts).thenReturn(listOf(
            mockBundlePart,
            mockVersionPart,
            mockApplicationIdPart,
            mockVariantPart,
            mockSigningConfigPart
        ))

        val executable = { uploadBundlePathHandler.handle(mockRequest, mockResponse) }

        val thrown = assertThrows<HttpException>(executable)
        assertThat(thrown.code).isEqualTo(HttpStatus.BAD_REQUEST_400)
        assertThat(thrown.message).contains("keystore")
    }

    @Test
    fun whenRequestIsValid_handle_shouldDelegateToApkSetManager() {
        val bundleInputStream = FakeInputStream("bundle")
        val mockBundlePart = mock<Part> {
            on { name } doReturn "bundle"
            on { inputStream } doReturn bundleInputStream
        }
        val mockApplicationIdPart = mock<Part> {
            on { name } doReturn "application-id"
            on { inputStream } doReturn FakeInputStream("application-id")
        }
        val mockVariantPart = mock<Part> {
            on { name } doReturn "variant"
            on { inputStream } doReturn FakeInputStream("variant")
        }
        val mockVersionPart = mock<Part> {
            on { name } doReturn "version"
            on { inputStream } doReturn FakeInputStream("23")
        }
        val mockSigningConfigPart = mock<Part> {
            on { name } doReturn "signing-config"
            on { inputStream } doReturn FakeInputStream("signingConfig")
        }
        val keyStoreInputStream = FakeInputStream("keystore")
        val mockKeystorePart = mock<Part> {
            on { name } doReturn "keystore"
            on { inputStream } doReturn keyStoreInputStream
        }
        whenever(mockRequest.contentType).thenReturn(UploadBundlePathHandler.CONTENT_TYPE_MULTIPART_FORM_DATA)
        whenever(mockRequest.parts).thenReturn(listOf(
            mockBundlePart,
            mockVersionPart,
            mockApplicationIdPart,
            mockVariantPart,
            mockSigningConfigPart,
            mockKeystorePart
        ))

        uploadBundlePathHandler.handle(mockRequest, mockResponse)

        verify(mockBundleManager).storeBundle(
            "application-id",
            23,
            "variant",
            "signingConfig",
            bundleInputStream,
            keyStoreInputStream
        )
    }
}