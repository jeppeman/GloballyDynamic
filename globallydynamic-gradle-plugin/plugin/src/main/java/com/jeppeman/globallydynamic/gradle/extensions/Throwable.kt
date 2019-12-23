package com.jeppeman.globallydynamic.gradle.extensions

import java.io.PrintWriter
import java.io.StringWriter

internal fun Throwable.stackTraceToString(): String {
    val sw = StringWriter()
    printStackTrace(PrintWriter(sw))
    return sw.toString()
}