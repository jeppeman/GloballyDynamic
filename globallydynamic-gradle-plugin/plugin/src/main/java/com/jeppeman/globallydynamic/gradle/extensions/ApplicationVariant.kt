package com.jeppeman.globallydynamic.gradle.extensions

import com.android.build.gradle.api.ApplicationVariant

fun ApplicationVariant.getTaskName(prefix: String, suffix: String = ""): String {
    val stringBuilder = StringBuilder(prefix)
    if (prefix.isBlank()) {
        stringBuilder.append(name)
    } else {
        stringBuilder.append(name.capitalize())
    }

    stringBuilder.append(suffix.capitalize())

    return stringBuilder.toString()
}
