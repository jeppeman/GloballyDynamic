package com.jeppeman.globallydynamic.idea.extensions

import com.android.tools.idea.gradle.project.facet.gradle.GradleFacet
import com.intellij.openapi.module.Module

val Module.hasGloballyDynamicEnabled: Boolean
    get() = GradleFacet.getInstance(this)
            ?.gradleModuleModel
            ?.gradlePlugins
            ?.contains("com.jeppeman.globallydynamic.gradle.GloballyDynamicPlugin") == true
