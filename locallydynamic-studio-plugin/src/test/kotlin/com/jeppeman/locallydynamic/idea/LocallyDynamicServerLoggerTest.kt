package com.jeppeman.locallydynamic.idea

import com.google.common.truth.Truth.assertThat
import com.intellij.execution.process.ProcessOutputTypes
import com.jeppeman.locallydynamic.server.extensions.stackTraceToString
import com.nhaarman.mockitokotlin2.spy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import org.mockito.junit.jupiter.MockitoExtension

@RunWith(JUnitPlatform::class)
@ExtendWith(MockitoExtension::class)
class LocallyDynamicServerLoggerTest {
    private lateinit var locallyDynamicServerLogger: LocallyDynamicServerLogger

    @BeforeEach
    fun setUp() {
        locallyDynamicServerLogger = spy(LocallyDynamicServerLogger())
    }

    @Test
    fun i_shouldNotifyListeners() {
        var message: String? = null
        locallyDynamicServerLogger.registerUpdateListener { message = it }

        locallyDynamicServerLogger.i("Hello there")

        assertThat(message).contains("${ProcessOutputTypes.STDOUT}/Hello there")
    }

    @Test
    fun e_shouldNotifyListeners() {
        var message: String? = null
        locallyDynamicServerLogger.registerUpdateListener { message = it }

        locallyDynamicServerLogger.e("Hello there")

        assertThat(message).contains("${ProcessOutputTypes.STDERR}/Hello there")
    }

    @Test
    fun whenArgumentIsException_e_shouldNotifyListeners() {
        var message: String? = null
        val exception = IllegalArgumentException("Hello there")
        locallyDynamicServerLogger.registerUpdateListener { message = it }

        locallyDynamicServerLogger.e(exception)

        assertThat(message).contains("${ProcessOutputTypes.STDERR}/${exception.stackTraceToString()}")
    }

    @Test
    fun unregisterUpdateListener_shouldUnregister() {
        var listenerCalledCounter = 0
        val listener: (String) -> Unit = {
            listenerCalledCounter++
        }
        locallyDynamicServerLogger.registerUpdateListener(listener)

        locallyDynamicServerLogger.i("Hello there")
        locallyDynamicServerLogger.unregisterUpdateListener(listener)
        locallyDynamicServerLogger.i("Hello there")

        assertThat(listenerCalledCounter).isEqualTo(1)
    }
}