package com.jeppeman.globallydynamic.gradle.generators

import com.android.tools.build.bundletool.model.ManifestDeliveryElement
import com.squareup.javapoet.*
import java.io.File
import javax.lang.model.element.Modifier

internal class GloballyDynamicBuildConfigGenerator(
    outputFile: File,
    installTimeFeatures: Map<String, ManifestDeliveryElement>,
    onDemandFeatures: Map<String, ManifestDeliveryElement>,
    serverUrl: String?,
    applicationId: String,
    mainActivityFullyQualifiedName: String?,
    version: Int?,
    variantName: String,
    throttleDownloadBy: Long
) : Generator(outputFile) {
    private val staticInitializer = CodeBlock.builder().apply {
        installTimeFeatures.forEach { (name, manifestDeliveryElement) ->
            val codeBlock = CodeBlock.builder()
            codeBlock.add("\$L.put(\$S, ", INSTALL_TIME_FEATURES_NAME, name)
            if (manifestDeliveryElement.hasModuleConditions()) {
                codeBlock.add("new \$T(\n", TypeNames.MODULE_CONDITIONS)
                    .indent()
                    .add("new \$T {\n", ArrayTypeName.of(TypeNames.DEVICE_FEATURE_CONDITION))
                    .indent()
                    .apply {
                        manifestDeliveryElement.moduleConditions
                            .deviceFeatureConditions
                            .forEachIndexed { index, deviceFeatureCondition ->
                                val suffix = if (index < manifestDeliveryElement.moduleConditions
                                        .deviceFeatureConditions
                                        .size - 1) {
                                    ",\n"
                                } else {
                                    "\n"
                                }
                                codeBlock.add(
                                    "new \$T(\$S, \$L)\$L",
                                    TypeNames.DEVICE_FEATURE_CONDITION,
                                    deviceFeatureCondition.featureName,
                                    deviceFeatureCondition.featureVersion.takeIf { it.isPresent }?.get(),
                                    suffix
                                )
                            }
                    }
                    .unindent()
                    .add("},\n")
                    .add("\$L,\n", if (manifestDeliveryElement.moduleConditions.minSdkVersion.isPresent) {
                        manifestDeliveryElement.moduleConditions.minSdkVersion.get()
                    } else {
                        "null"
                    })

                if (manifestDeliveryElement.moduleConditions.userCountriesCondition.isPresent) {
                    val userCountriesCondition = manifestDeliveryElement.moduleConditions
                        .userCountriesCondition
                        .get()

                    codeBlock.add(
                        "new \$T(new \$T { \$L }, \$L)",
                        TypeNames.USER_COUNTRIES_CONDITION,
                        ArrayTypeName.of(TypeNames.STRING),
                        userCountriesCondition.countries
                            .joinToString(", ") { country -> "\"$country\"" },
                        userCountriesCondition.exclude
                    )
                } else {
                    codeBlock.add("null")
                }

                codeBlock.add("\n")
                    .unindent()
                    .add(")")
            } else {
                codeBlock.add("null")
            }

            codeBlock.add(");")
            add(codeBlock.build())
            add("\n")
        }
    }.build()

    private val installTimeFeaturesField = FieldSpec.builder(
        ParameterizedTypeName.get(
            TypeNames.MAP,
            TypeNames.STRING,
            TypeNames.MODULE_CONDITIONS
        ),
        INSTALL_TIME_FEATURES_NAME,
        Modifier.STATIC, Modifier.PRIVATE, Modifier.FINAL)
        .initializer("new \$T()", ParameterizedTypeName.get(
            TypeNames.HASH_MAP,
            TypeNames.STRING,
            TypeNames.MODULE_CONDITIONS
        ))
        .build()

    private val onDemandFeaturesField = FieldSpec.builder(
        ArrayTypeName.of(String::class.java),
        ON_DEMAND_FEATURES_NAME,
        Modifier.STATIC, Modifier.PRIVATE, Modifier.FINAL)
        .initializer(
            "new \$T { \$L }",
            ArrayTypeName.of(String::class.java),
            onDemandFeatures.keys.joinToString(", ") { feature -> "\"$feature\"" }
        )
        .build()

    private val serverUrlField = FieldSpec.builder(
        TypeNames.STRING,
        SERVER_URL_NAME,
        Modifier.STATIC, Modifier.PRIVATE, Modifier.FINAL)
        .initializer("\$S", serverUrl ?: "")
        .build()

    private val applicationIdField = FieldSpec.builder(
        TypeNames.STRING,
        APPLICATION_ID_NAME,
        Modifier.STATIC, Modifier.PRIVATE, Modifier.FINAL)
        .initializer("\$S", applicationId)
        .build()

    private val mainActivityFullyQualifiedNameField = FieldSpec.builder(
        TypeNames.STRING,
        MAIN_ACTIVITY_FULLY_QUALIFIED_NAME_NAME,
        Modifier.STATIC, Modifier.PRIVATE, Modifier.FINAL)
        .initializer("\$L", if (mainActivityFullyQualifiedName != null) {
            "\"$mainActivityFullyQualifiedName\""
        } else {
            "null"
        })
        .build()

    private val variantNameField = FieldSpec.builder(
        TypeNames.STRING,
        VARIANT_NAME_NAME,
        Modifier.STATIC, Modifier.PRIVATE, Modifier.FINAL)
        .initializer("\$S", variantName)
        .build()

    private val throttleDownloadByField = FieldSpec.builder(
        TypeName.LONG,
        THROTTLE_DOWNLOAD_BY_NAME,
        Modifier.STATIC, Modifier.PRIVATE, Modifier.FINAL)
        .initializer("\$L", throttleDownloadBy)
        .build()

    private val versionCodeField = FieldSpec.builder(
        TypeName.INT,
        VERSION_CODE_NAME,
        Modifier.STATIC, Modifier.PRIVATE, Modifier.FINAL)
        .initializer("\$L", version)
        .build()

    override val typeSpec: TypeSpec = TypeSpec.classBuilder(TypeNames.GLOBALLY_DYNAMIC_BUILD_CONFIG)
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        .addStaticBlock(staticInitializer)
        .addFields(listOf(
            installTimeFeaturesField,
            onDemandFeaturesField,
            serverUrlField,
            applicationIdField,
            mainActivityFullyQualifiedNameField,
            variantNameField,
            throttleDownloadByField,
            versionCodeField
        ))
        .addMethods(listOf(
            installTimeFeaturesField.asGetter("installTimeFeatures"),
            onDemandFeaturesField.asGetter("onDemandFeatures"),
            serverUrlField.asGetter("serverUrl"),
            applicationIdField.asGetter("applicationId"),
            mainActivityFullyQualifiedNameField.asGetter("mainActivityFullyQualifiedName"),
            variantNameField.asGetter("variantName"),
            throttleDownloadByField.asGetter("throttleDownloadBy"),
            versionCodeField.asGetter("versionCode")
        ))
        .build()

    companion object {
        private const val INSTALL_TIME_FEATURES_NAME = "INSTALL_TIME_FEATURES"
        private const val ON_DEMAND_FEATURES_NAME = "ON_DEMAND_FEATURES"
        private const val SERVER_URL_NAME = "SERVER_URL"
        private const val APPLICATION_ID_NAME = "APPLICATION_ID"
        private const val VARIANT_NAME_NAME = "VARIANT_NAME"
        private const val VERSION_CODE_NAME = "VERSION_CODE"
        private const val THROTTLE_DOWNLOAD_BY_NAME = "THROTTLE_DOWNLOAD_BY"
        private const val MAIN_ACTIVITY_FULLY_QUALIFIED_NAME_NAME = "MAIN_ACTIVITY_FULLY_QUALIFIED_NAME"
    }
}