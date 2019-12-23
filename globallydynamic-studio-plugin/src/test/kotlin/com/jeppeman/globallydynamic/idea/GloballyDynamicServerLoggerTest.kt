package com.jeppeman.globallydynamic.idea

import com.google.common.truth.Truth.assertThat
import com.intellij.execution.process.ProcessOutputTypes
import com.jeppeman.globallydynamic.server.extensions.stackTraceToString
import com.nhaarman.mockitokotlin2.spy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import org.mockito.junit.jupiter.MockitoExtension

@RunWith(JUnitPlatform::class)
@ExtendWith(MockitoExtension::class)
class GloballyDynamicServerLoggerTest {
    private lateinit var globallyDynamicServerLogger: GloballyDynamicServerLogger

    @BeforeEach
    fun setUp() {
        globallyDynamicServerLogger = spy(GloballyDynamicServerLogger())
    }

    @Test
    fun i_shouldNotifyListeners() {
        var message: String? = null
        globallyDynamicServerLogger.registerUpdateListener { message = it }

        globallyDynamicServerLogger.i("Hello there")

        assertThat(message).contains("${ProcessOutputTypes.STDOUT}/Hello there")
    }

    @Test
    fun e_shouldNotifyListeners() {
        var message: String? = null
        globallyDynamicServerLogger.registerUpdateListener { message = it }

        globallyDynamicServerLogger.e("Hello there")

        assertThat(message).contains("${ProcessOutputTypes.STDERR}/Hello there")
    }

    @Test
    fun whenArgumentIsException_e_shouldNotifyListeners() {
        var message: String? = null
        val exception = IllegalArgumentException("Hello there")
        globallyDynamicServerLogger.registerUpdateListener { message = it }

        globallyDynamicServerLogger.e(exception)

        assertThat(message).contains("${ProcessOutputTypes.STDERR}/${exception.stackTraceToString()}")
    }

    @Test
    fun unregisterUpdateListener_shouldUnregister() {
        var listenerCalledCounter = 0
        val listener: (String) -> Unit = {
            listenerCalledCounter++
        }
        globallyDynamicServerLogger.registerUpdateListener(listener)

        globallyDynamicServerLogger.i("Hello there")
        globallyDynamicServerLogger.unregisterUpdateListener(listener)
        globallyDynamicServerLogger.i("Hello there")

        assertThat(listenerCalledCounter).isEqualTo(1)
    }
}