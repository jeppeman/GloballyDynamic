package com.jeppeman.locallydynamic.server.extensions

import java.io.PrintWriter
import java.io.StringWriter

fun Throwable.stackTraceToString(): String {
    val sw = StringWriter()
    printStackTrace(PrintWriter(sw))
    return sw.toString()
}