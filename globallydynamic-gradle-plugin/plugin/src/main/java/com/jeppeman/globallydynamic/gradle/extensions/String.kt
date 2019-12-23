package com.jeppeman.globallydynamic.gradle.extensions

import java.util.*

internal fun String.toBase64(): String = Base64.getUrlEncoder().withoutPadding().encodeToString(toByteArray(Charsets.UTF_8))