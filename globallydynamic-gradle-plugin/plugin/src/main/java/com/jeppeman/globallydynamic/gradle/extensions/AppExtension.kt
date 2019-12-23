package com.jeppeman.globallydynamic.gradle.extensions

import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.api.LibraryVariant
import com.jeppeman.globallydynamic.gradle.GloballyDynamicServersExtension

/**
 * Tries to find a matching [ApplicationVariant] in a project based on [applicationVariant].
 * If no matches are found it tries to find a fallback based on the provided [buildTypeFallbacks]
 */
internal fun AppExtension.findMatchingVariant(
    applicationVariant: ApplicationVariant,
    buildTypeFallbacks: List<String>
): ApplicationVariant? {
    val maybeMatch = applicationVariants.firstOrNull { variant -> variant.name == applicationVariant.name }
    if (maybeMatch != null) {
        return maybeMatch
    }

    val maybeFallback = buildTypeFallbacks.firstOrNull { fallback ->
        applicationVariants.map { it.buildType.name }.contains(fallback)
    }

    if (maybeFallback != null) {
        val maybeFallbackMatch = applicationVariants.firstOrNull { variant ->
            variant.buildType.name == maybeFallback && variant.flavorName == applicationVariant.flavorName
        }

        if (maybeFallbackMatch != null) {
            return maybeFallbackMatch
        }
    }

    return applicationVariants.firstOrNull { variant -> variant.buildType.name == applicationVariant.buildType.name }
}

internal fun LibraryExtension.findMatchingVariant(
    applicationVariant: ApplicationVariant,
    buildTypeFallbacks: List<String>
): LibraryVariant? {
    val maybeMatch = libraryVariants.firstOrNull { variant -> variant.name == applicationVariant.name }
    if (maybeMatch != null) {
        return maybeMatch
    }

    val maybeFallback = buildTypeFallbacks.firstOrNull { fallback ->
        libraryVariants.map { it.buildType.name }.contains(fallback)
    }

    if (maybeFallback != null) {
        val maybeFallbackMatch = libraryVariants.firstOrNull { variant ->
            variant.buildType.name == maybeFallback && variant.flavorName == applicationVariant.flavorName
        }

        if (maybeFallbackMatch != null) {
            return maybeFallbackMatch
        }
    }

    return null
}

fun AppExtension.globallyDynamicServers(block: GloballyDynamicServersExtension.() -> Unit) {
    extension<GloballyDynamicServersExtension>().apply(block)
}

