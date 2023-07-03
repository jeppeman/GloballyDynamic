package com.jeppeman.globallydynamic.server

import com.jeppeman.globallydynamic.server.extensions.toBase64
import org.mockito.kotlin.*
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
import javax.servlet.ServletOutputStream
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
    @Mock
    private lateinit var mockOutputStream: ServletOutputStream
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
            configuration = GloballyDynamicServer.Configuration.builder()
                .setUsername(username)
                .setPassword(password)
                .setLogger(mockLogger)
                .setHttpsRedirect(false)
                .build(),
            pathHandlers = pathHandlers
        )

        whenever(mockRequest.getHeader("X-Forwarded-Proto")).thenReturn("http")
        whenever(mockRequest.scheme).thenReturn("http")
        whenever(mockResponse.outputStream).thenReturn(mockOutputStream)
    }

    @Test
    fun whenCredentialsAreMissing_handle_shouldRespondWith401() {
        whenever(mockRequest.pathInfo).thenReturn("/path")
        requestHandler.handle("", mockBaserequest, mockRequest, mockResponse)

        verify(mockOutputStream).write(any<ByteArray>())
        verify(mockResponse).status = HttpStatus.UNAUTHORIZED_401
    }

    @Test
    fun whenCredentialsAreInvalid_handle_shouldRespondWith401() {
        whenever(mockRequest.pathInfo).thenReturn("/path")
        whenever(mockRequest.getHeader("Authorization")).thenReturn("Basic invalid")

        requestHandler.handle("", mockBaserequest, mockRequest, mockResponse)

        verify(mockResponse).status = HttpStatus.UNAUTHORIZED_401
    }

    @Test
    fun whenPathIsNotRegistered_handle_shouldRespondWith404() {
        val auth = "$username:$password".toBase64()
        whenever(mockRequest.getHeader("Authorization")).thenReturn("Basic $auth")

        requestHandler.handle("", mockBaserequest, mockRequest, mockResponse)

        verify(mockResponse).status = HttpStatus.NOT_FOUND_404
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
            configuration = GloballyDynamicServer.Configuration.builder()
                .setUsername(username)
                .setPassword(password)
                .setLogger(mockLogger)
                .setHttpsRedirect(true)
                .build(),
            pathHandlers = pathHandlers
        )
        val url = "http://url.com"
        whenever(mockRequest.requestURL).thenReturn(StringBuffer(url))

        requestHandler.handle("", mockBaserequest, mockRequest, mockResponse)

        verify(mockResponse).sendRedirect(url.replace("http://", "https://"))
    }
}