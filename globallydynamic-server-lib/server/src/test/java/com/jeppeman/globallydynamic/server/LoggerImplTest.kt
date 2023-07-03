package com.jeppeman.globallydynamic.server

import com.google.common.truth.Truth.assertThat
import com.jeppeman.globallydynamic.server.Logger
import com.jeppeman.globallydynamic.server.extensions.stackTraceToString
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.io.PrintStream

@RunWith(JUnitPlatform::class)
@ExtendWith(MockitoExtension::class)
class LoggerImplTest {
    @Mock
    private lateinit var mockOutStream: PrintStream
    @Mock
    private lateinit var mockErrorStream: PrintStream
    private lateinit var systemOut: PrintStream
    private lateinit var systemErr: PrintStream
    private val logger = Logger()

    @BeforeEach
    fun setUp() {
        systemOut = System.out
        systemErr = System.err
        System.setOut(mockOutStream)
        System.setErr(mockErrorStream)
    }

    @AfterEach
    fun tearDown() {
        System.setOut(systemOut)
        System.setErr(systemErr)
    }

    @Test
    fun i_shouldAddTagAndPrintlnToSystemOut() {
        val messageCaptor = argumentCaptor<Any>()
        val message = "Hello there"

        logger.i(message)

        verify(mockOutStream).println(messageCaptor.capture())
        assertThat(messageCaptor.firstValue.toString()).endsWith("GloballyDynamicServer: $message")
    }

    @Test
    fun whenArgumentIsPlainText_e_shouldAddTagAndPrintlnToSystemErr() {
        val messageCaptor = argumentCaptor<String>()
        val message = "Hello there, this is an error message"

        logger.e(message)

        verify(mockErrorStream).println(messageCaptor.capture())
        assertThat(messageCaptor.firstValue).endsWith("GloballyDynamicServer: $message")
    }

    @Test
    fun whenArgumentThrowable_e_shouldAddStacktraceAndTagAndPrintlnToSystemErr() {
        val messageCaptor = argumentCaptor<String>()
        val illegalStateException = IllegalStateException()

        logger.e(illegalStateException)

        verify(mockErrorStream).println(messageCaptor.capture())
        assertThat(messageCaptor.firstValue).endsWith("GloballyDynamicServer: ${illegalStateException.stackTraceToString()}")
    }

    @Test
    fun whenArgumentsAreMessageAndThrowable_e_shouldAddMessageAndStacktraceAndTagAndPrintlnToSystemErr() {
        val messageCaptor = argumentCaptor<String>()
        val message = "Hello there, this is an error message"
        val illegalStateException = IllegalStateException()

        logger.e(message, illegalStateException)

        verify(mockErrorStream).println(messageCaptor.capture())
        assertThat(messageCaptor.firstValue).endsWith("GloballyDynamicServer: $message: ${illegalStateException.stackTraceToString()}")
    }
}