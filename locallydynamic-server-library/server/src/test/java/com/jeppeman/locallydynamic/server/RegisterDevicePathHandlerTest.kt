package com.jeppeman.locallydynamic.server

import com.google.common.truth.Truth.assertThat
import com.google.gson.GsonBuilder
import com.jeppeman.locallydynamic.server.dto.DeviceSpecDto
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.eclipse.jetty.http.HttpMethod
import org.eclipse.jetty.http.HttpStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import javax.servlet.ServletOutputStream
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@RunWith(JUnitPlatform::class)
@ExtendWith(MockitoExtension::class)
class RegisterDevicePathHandlerTest {
    @Mock
    private lateinit var mockDeviceManager: DeviceManager
    @Mock
    private lateinit var mockLogger: Logger
    @Mock
    private lateinit var mockRequest: HttpServletRequest
    @Mock
    private lateinit var mockResponse: HttpServletResponse
    private lateinit var registerDevicePathHandler: RegisterDevicePathHandler
    private val gson = GsonBuilder().create()

    @BeforeEach
    fun setUp() {
        registerDevicePathHandler = RegisterDevicePathHandler(
            deviceManager = mockDeviceManager,
            gson = gson,
            logger = mockLogger
        )
    }

    @Test
    fun whenMethodIsNotPost_handle_shouldThrowWith400() {
        val executable = { registerDevicePathHandler.handle(mockRequest, mockResponse) }

        val thrown = assertThrows<HttpException>(executable)
        assertThat(thrown.code).isEqualTo(HttpStatus.BAD_REQUEST_400)
    }

    @Test
    fun whenBodyIsInvalid_handle_shouldThrowWith400() {
        whenever(mockRequest.method).thenReturn(HttpMethod.POST.asString())
        whenever(mockRequest.inputStream).thenReturn(FakeInputStream("invalid body"))
        val executable = { registerDevicePathHandler.handle(mockRequest, mockResponse) }

        val thrown = assertThrows<HttpException>(executable)
        assertThat(thrown.code).isEqualTo(HttpStatus.BAD_REQUEST_400)
    }

    @Test
    fun whenBodyIsValid_handle_shouldWriteDeviceId() {
        val deviceSpecDto = DeviceSpecDto(
            supportedAbis = listOf("a", "b", "c"),
            supportedLocales = listOf("aa", "bb", "cc"),
            deviceFeatures = listOf("aaa", "bbb", "ccc"),
            glExtensions = listOf("aaaa", "bbbb", "cccc"),
            screenDensity = 420,
            sdkVersion = 23
        )
        val deviceId = "deviceId"
        val mockOutputStream = mock<ServletOutputStream>()
        whenever(mockRequest.method).thenReturn(HttpMethod.POST.asString())
        whenever(mockRequest.inputStream).thenReturn(FakeInputStream(
            gson.toJson(deviceSpecDto)
        ))
        whenever(mockResponse.outputStream).thenReturn(mockOutputStream)
        whenever(mockDeviceManager.registerDevice(any())).thenReturn(deviceId)

        registerDevicePathHandler.handle(mockRequest, mockResponse)

        verify(mockResponse).contentType = "text/plain; charset=utf8"
        verify(mockResponse).setContentLength(deviceId.length)
        verify(mockOutputStream).write(deviceId.toByteArray())
    }
}