package com.jeppeman.globallydynamic.server

import com.jeppeman.globallydynamic.server.extensions.stackTraceToString
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

interface Logger {
    fun i(message: String, newLine: Boolean = true, prefix: String = "")
    fun e(message: String, newLine: Boolean = true, prefix: String = "")
    fun e(throwable: Throwable, newLine: Boolean = true, prefix: String = "")
    fun e(message: String, throwable: Throwable, newLine: Boolean = true, prefix: String = "")

    companion object : () -> Logger {
        override fun invoke(): Logger = LoggerImpl()
    }
}

private class LoggerImpl : Logger {
    private val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern(
        "yyyy-MM-dd HH:mm:ss",
        Locale.getDefault()
    )

    private fun String.timestamped(): String {
        return "${LocalDateTime.now().format(dateTimeFormatter)} GloballyDynamicServer: $this"
    }

    override fun i(message: String, newLine: Boolean, prefix: String) {
        if (newLine) {
            println("${prefix}${message.timestamped()}")
        } else {
            print("\r$prefix${message.timestamped()}")
        }
    }

    override fun e(message: String, newLine: Boolean, prefix: String) {
        if (newLine) {
            System.err.println("$prefix${message.timestamped()}")
        } else {
            System.err.print("\r$prefix${message.timestamped()}")
        }
    }

    override fun e(throwable: Throwable, newLine: Boolean, prefix: String) {
        e(throwable.stackTraceToString(), newLine)
    }

    override fun e(message: String, throwable: Throwable, newLine: Boolean, prefix: String) {
        e("$message: ${throwable.stackTraceToString()}", newLine)
    }
}