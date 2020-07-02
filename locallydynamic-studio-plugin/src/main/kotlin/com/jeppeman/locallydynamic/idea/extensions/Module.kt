package com.jeppeman.locallydynamic.idea.extensions

import com.android.tools.idea.gradle.project.facet.gradle.GradleFacet
import com.intellij.openapi.module.Module

val Module.hasLocallyDynamicEnabled: Boolean
    get() = GradleFacet.getInstance(this)
            ?.gradleModuleModel
            ?.gradlePlugins
            ?.contains("com.jeppeman.locallydynamic.gradle.LocallyDynamicPlugin") == true
