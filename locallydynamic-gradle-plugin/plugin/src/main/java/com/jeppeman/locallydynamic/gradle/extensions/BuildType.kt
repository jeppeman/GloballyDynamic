package com.jeppeman.locallydynamic.gradle.extensions

import com.android.build.gradle.internal.dsl.BuildType
import com.jeppeman.locallydynamic.gradle.LocallyDynamicExtension
import org.gradle.api.plugins.ExtensionAware

fun BuildType.locallyDynamic(block: LocallyDynamicExtension.() -> Unit) {
    val extension = (this as ExtensionAware).extensions.getByType(LocallyDynamicExtension::class.java)
    extension.apply(block)
}
