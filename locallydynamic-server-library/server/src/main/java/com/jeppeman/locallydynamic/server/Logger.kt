package com.jeppeman.locallydynamic.server

import com.jeppeman.locallydynamic.server.extensions.stackTraceToString
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

interface Logger {
    fun i(message: String)
    fun e(message: String)
    fun e(throwable: Throwable)
    fun e(message: String, throwable: Throwable)

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
        return "${LocalDateTime.now().format(dateTimeFormatter)} LocallyDynamicServer: $this"
    }

    override fun i(message: String) {
        println(message.timestamped())
    }

    override fun e(message: String) {
        System.err.println(message.timestamped())
    }

    override fun e(throwable: Throwable) {
        e(throwable.stackTraceToString())
    }

    override fun e(message: String, throwable: Throwable) {
        e("$message: ${throwable.stackTraceToString()}")
    }
}