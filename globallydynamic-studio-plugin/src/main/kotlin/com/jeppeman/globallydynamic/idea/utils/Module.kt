package com.jeppeman.globallydynamic.idea.utils

import com.android.tools.idea.gradle.project.facet.gradle.GradleFacet
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootManager
import org.jetbrains.android.facet.AndroidFacet
import java.io.File

val Module.gradleFacet: GradleFacet? get() = GradleFacet.getInstance(this)

val GradleFacet.globallyDynamicFolder: File?
    get() = gradleModuleModel
        ?.buildFilePath
        ?.parentFile
        ?.resolve("build")
        ?.resolve(GLOBALLY_DYNAMIC_DIR)


val Module.basePaths: Set<String>
    get() = ModuleRootManager.getInstance(this)
        .contentRoots
        .mapNotNull { it.canonicalPath }
        .toSet()

val Module.isAppProject: Boolean
    get() = AndroidFacet.getInstance(this)
        ?.configuration
        ?.isAppProject == true

private const val GLOBALLY_DYNAMIC_DIR = "globallydynamic"