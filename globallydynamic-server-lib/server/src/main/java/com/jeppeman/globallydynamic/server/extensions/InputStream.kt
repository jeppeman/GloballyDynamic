package com.jeppeman.globallydynamic.server.extensions

import java.io.InputStream
import java.nio.charset.Charset

fun InputStream.readString(charset: Charset = Charsets.UTF_8) = readBytes().toString(charset)