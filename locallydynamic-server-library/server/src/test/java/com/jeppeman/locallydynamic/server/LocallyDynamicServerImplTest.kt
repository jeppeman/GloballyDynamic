package com.jeppeman.locallydynamic.server

import com.google.common.truth.Truth.assertThat
import com.google.gson.GsonBuilder
import com.nhaarman.mockitokotlin2.*
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
class LocallyDynamicServerImplTest {
    @Mock
    private lateinit var mockServer: Server
    @Mock
    private lateinit var mockDeviceManager: DeviceManager
    @Mock
    private lateinit var mockBundleManager: BundleManager

    private lateinit var locallyDynamicServer: LocallyDynamicServerImpl
    private val configuration = LocallyDynamicServer.Configuration
        .builder()
        .setUsername("username")
        .setPassword("password")
        .build()

    @BeforeEach
    fun setUp() {
        locallyDynamicServer = LocallyDynamicServerImpl(
            gson = GsonBuilder().create(),
            configuration = configuration,
            deviceManager = mockDeviceManager,
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

        val address = locallyDynamicServer.start()

        verify(mockServer, never()).handler = any()
        assertThat(address).isEqualTo(uri.toString())
    }

    @Test
    fun whenServerIsNotStarted_start_shouldStartIt() {
        val uri = URI("address")
        doReturn(uri).whenever(mockServer).uri
        val captor = argumentCaptor<RequestHandler>()
        val address = locallyDynamicServer.start()

        verify(mockServer).start()
        verify(mockServer).handler = captor.capture()
        assertThat(captor.firstValue.logger).isSameAs(configuration.logger)
        assertThat(captor.firstValue.username).isEqualTo(configuration.username)
        assertThat(captor.firstValue.password).isEqualTo(configuration.password)
        assertThat(captor.firstValue.pathHandlers.first().path).isEqualTo("path")
        assertThat(address).isEqualTo(uri.toString())
    }

    @Test
    fun join_shouldDelegateToServer() {
        locallyDynamicServer.join()

        verify(mockServer).join()
    }

    @Test
    fun stop_shouldDelegateToServer() {
        val inOrder = inOrder(mockServer)

        locallyDynamicServer.stop()

        inOrder.verify(mockServer).stop()
        inOrder.verify(mockServer).destroy()
    }
}