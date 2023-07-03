package com.jeppeman.globallydynamic.server

import com.google.common.truth.Truth.assertThat
import com.google.gson.GsonBuilder
import org.mockito.kotlin.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.net.URI
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@RunWith(JUnitPlatform::class)
@ExtendWith(MockitoExtension::class)
class GloballyDynamicServerImplTest {
    @Mock
    private lateinit var mockServer: Server
    @Mock
    private lateinit var mockBundleManager: BundleManager

    private lateinit var globallyDynamicServerImpl: GloballyDynamicServerImpl
    private val configuration = GloballyDynamicServer.Configuration
        .builder()
        .setUsername("username")
        .setPassword("password")
        .build()

    @BeforeEach
    fun setUp() {
        globallyDynamicServerImpl = GloballyDynamicServerImpl(
            gson = GsonBuilder().create(),
            configuration = configuration,
            bundleManager = mockBundleManager,
            server = mockServer,
            lazyPathHandlers = {
                listOf(
                    object : PathHandler {
                        override val path: String = "path"
                        override fun handle(request: HttpServletRequest?, response: HttpServletResponse?) = Unit
                    }
                )
            }
        )
    }

    @Test
    fun whenServerIsStarted_start_shouldOnlyReturnAddress() {
        val uri = URI("address")
        doReturn(uri).whenever(mockServer).uri
        doReturn(true).whenever(mockServer).isStarted

        val address = globallyDynamicServerImpl.start()

        verify(mockServer, never()).handler = any()
        assertThat(address).isEqualTo(uri.toString())
    }

    @Test
    fun whenServerIsNotStarted_start_shouldStartIt() {
        val uri = URI("address")
        doReturn(uri).whenever(mockServer).uri
        val captor = argumentCaptor<RequestHandler>()
        val address = globallyDynamicServerImpl.start()

        verify(mockServer).start()
        verify(mockServer).handler = captor.capture()
        assertThat(captor.firstValue.configuration.logger).isSameAs(configuration.logger)
        assertThat(captor.firstValue.configuration.username).isEqualTo(configuration.username)
        assertThat(captor.firstValue.configuration.password).isEqualTo(configuration.password)
        assertThat(captor.firstValue.pathHandlers.first().path).isEqualTo("path")
        assertThat(address).isEqualTo(uri.toString())
    }

    @Test
    fun join_shouldDelegateToServer() {
        globallyDynamicServerImpl.join()

        verify(mockServer).join()
    }

    @Test
    fun stop_shouldDelegateToServer() {
        val inOrder = inOrder(mockServer)

        globallyDynamicServerImpl.stop()

        inOrder.verify(mockServer).stop()
        inOrder.verify(mockServer).destroy()
    }
}