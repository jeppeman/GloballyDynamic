package com.jeppeman.globallydynamic.gradle.extensions

import com.android.build.gradle.api.ApplicationVariant
import org.gradle.api.Project
import java.io.File

fun Project.intermediaryBundleDir(variant: ApplicationVariant): File = buildDir
    .toPath()
    .resolve("intermediates")
    .resolve("intermediary_bundle")
    .resolve(variant.name)
    .resolve(variant.getTaskName("package", "Bundle"))
    .toFile()

fun Project.intermediarySigningConfig(variant: ApplicationVariant): File = buildDir
    .toPath()
    .resolve("intermediates")
    .resolve("signing_config_data")
    .resolve(variant.name)
    .resolve(variant.getTaskName("signingConfigWriter"))
    .resolve("signing-config-data.json")
    .toFile()