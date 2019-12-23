package com.jeppeman.globallydynamic.gradle.extensions

import org.gradle.api.plugins.ExtensionAware

internal inline fun <reified T> Any.extension(): T = (this as ExtensionAware).extensions.getByType(T::class.java)