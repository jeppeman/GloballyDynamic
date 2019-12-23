package com.jeppeman.locallydynamic.server

import com.jeppeman.locallydynamic.server.extensions.toBase64
import com.nhaarman.mockitokotlin2.*
import org.eclipse.jetty.http.HttpStatus
import org.eclipse.jetty.server.Request
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@RunWith(JUnitPlatform::class)
@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RequestHandlerTest {
    @Mock
    private lateinit var mockLogger: Logger
    @Mock
    private lateinit var mockRequest: HttpServletRequest
    @Mock
    private lateinit var mockResponse: HttpServletResponse
    @Mock
    private lateinit var mockBaserequest: Request
    private lateinit var requestHandler: RequestHandler

    private val pathHandlers = listOf<PathHandler>(mock {
        on { path } doReturn "path"
        on { authRequired } doReturn true
    })
    private val username = "username"
    private val password = "password"

    @BeforeEach
    fun setUp() {
        requestHandler = RequestHandler(
            username = username,
            password = password,
            httpsRedirect = false,
            pathHandlers = pathHandlers,
            logger = mockLogger
        )

        whenever(mockRequest.getHeader("X-Forwarded-Proto")).thenReturn("http")
        whenever(mockRequest.scheme).thenReturn("http")
    }

    @Test
    fun whenCredentialsAreMissing_handle_shouldRespondWith401() {
        whenever(mockRequest.pathInfo).thenReturn("/path")
        requestHandler.handle("", mockBaserequest, mockRequest, mockResponse)

        verify(mockResponse).sendError(eq(HttpStatus.UNAUTHORIZED_401), any())
    }

    @Test
    fun whenCredentialsAreInvalid_handle_shouldRespondWith401() {
        whenever(mockRequest.pathInfo).thenReturn("/path")
        whenever(mockRequest.getHeader("Authorization")).thenReturn("Basic invalid")

        requestHandler.handle("", mockBaserequest, mockRequest, mockResponse)

        verify(mockResponse).sendError(eq(HttpStatus.UNAUTHORIZED_401), any())
    }

    @Test
    fun whenPathIsNotRegistered_handle_shouldRespondWith404() {
        val auth = "$username:$password".toBase64()
        whenever(mockRequest.getHeader("Authorization")).thenReturn("Basic $auth")

        requestHandler.handle("", mockBaserequest, mockRequest, mockResponse)

        verify(mockResponse).sendError(eq(HttpStatus.NOT_FOUND_404), any())
    }

    @Test
    fun whenPathHandlerIsRegistered_handle_shouldDelegateToIt() {
        val auth = "$username:$password".toBase64()
        whenever(mockRequest.getHeader("Authorization")).thenReturn("Basic $auth")
        whenever(mockRequest.pathInfo).thenReturn("/path")

        requestHandler.handle("", mockBaserequest, mockRequest, mockResponse)

        verify(pathHandlers[0]).handle(mockRequest, mockResponse)
    }

    @Test
    fun whenRequestIsSslAndHttpsRedirectIsTrue_handle_shouldRedirectToHttps() {
        requestHandler = RequestHandler(
            username = username,
            password = password,
            httpsRedirect = true,
            pathHandlers = pathHandlers,
            logger = mockLogger
        )
        val url = "http://url.com"
        whenever(mockRequest.requestURL).thenReturn(StringBuffer(url))

        requestHandler.handle("", mockBaserequest, mockRequest, mockResponse)

        verify(mockResponse).sendRedirect(url.replace("http://", "https://"))
    }
}