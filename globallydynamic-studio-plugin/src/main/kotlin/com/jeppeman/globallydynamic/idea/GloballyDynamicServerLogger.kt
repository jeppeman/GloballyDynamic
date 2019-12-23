package com.jeppeman.globallydynamic.idea

import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.util.containers.mapSmart
import com.jeppeman.globallydynamic.server.Logger
import com.jeppeman.globallydynamic.server.extensions.stackTraceToString
import org.apache.commons.collections.buffer.CircularFifoBuffer


class GloballyDynamicServerLogger : Logger {
    private val updateListeners = mutableListOf<(String) -> Unit>()
    private val buffer = CircularFifoBuffer(256)

    val content: List<String> get() = buffer.mapSmart { it.toString() }

    private fun addLine(message: String) {
        buffer.add(message)
        updateListeners.forEach { listener -> listener(message) }
    }

    fun registerUpdateListener(listener: (String) -> Unit) {
        updateListeners.add(listener)
    }

    fun unregisterUpdateListener(listener: (String) -> Unit) {
        updateListeners.remove(listener)
    }

    override fun e(message: String, throwable: Throwable, newLine: Boolean, prefix: String) {
        e("$message: ${throwable.stackTraceToString()}")
    }

    override fun e(throwable: Throwable, newLine: Boolean, prefix: String) {
        e(throwable.stackTraceToString())
    }

    override fun e(message: String, newLine: Boolean, prefix: String) {
        addLine("${ProcessOutputTypes.STDERR}/${message}")
    }

    override fun i(message: String, newLine: Boolean, prefix: String) {
        addLine("${ProcessOutputTypes.STDOUT}/${message}")
    }
}