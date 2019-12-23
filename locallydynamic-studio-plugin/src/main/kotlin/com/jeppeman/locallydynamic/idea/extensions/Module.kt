package com.jeppeman.locallydynamic.idea.extensions

import com.android.tools.idea.gradle.parser.GradleBuildFile
import com.intellij.openapi.module.Module

val Module.hasLocallyDynamicEnabled: Boolean
    get() = GradleBuildFile.get(this)
        ?.plugins
        ?.contains("com.jeppeman.locallydynamic") == true