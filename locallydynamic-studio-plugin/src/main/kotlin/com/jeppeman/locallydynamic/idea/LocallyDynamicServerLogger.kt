package com.jeppeman.locallydynamic.idea

import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.util.containers.mapSmart
import com.jeppeman.locallydynamic.server.Logger
import com.jeppeman.locallydynamic.server.extensions.stackTraceToString
import org.apache.commons.collections.buffer.CircularFifoBuffer


class LocallyDynamicServerLogger : Logger {
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

    override fun e(message: String, throwable: Throwable) {
        e("$message: ${throwable.stackTraceToString()}")
    }

    override fun e(throwable: Throwable) {
        e(throwable.stackTraceToString())
    }

    override fun e(message: String) {
        addLine("${ProcessOutputTypes.STDERR}/$message")
    }

    override fun i(message: String) {
        addLine("${ProcessOutputTypes.STDOUT}/$message")
    }
}