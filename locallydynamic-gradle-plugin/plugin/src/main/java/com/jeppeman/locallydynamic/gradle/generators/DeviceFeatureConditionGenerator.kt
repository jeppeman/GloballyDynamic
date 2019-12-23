package com.jeppeman.locallydynamic.gradle.generators

import com.squareup.javapoet.*
import java.io.File
import javax.lang.model.element.Modifier

class DeviceFeatureConditionGenerator(outputFile: File) : Generator(outputFile) {
    private val featureNameSpec = FieldSpec.builder(TypeNames.STRING, FEATURE_NAME_NAME)
        .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
        .build()

    private val featureVersionSpec = FieldSpec.builder(TypeNames.INTEGER, FEATURE_VERSION_NAME)
        .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
        .build()

    private val constructorSpec = MethodSpec.constructorBuilder()
        .addParameters(listOf(
            ParameterSpec.builder(
                TypeNames.STRING,
                FEATURE_NAME_NAME
            ).build(),
            ParameterSpec.builder(
                TypeNames.INTEGER,
                FEATURE_VERSION_NAME
            ).build()
        ))
        .addCode(
            CodeBlock.builder()
                .addStatement("this.\$L = \$L", FEATURE_NAME_NAME, FEATURE_NAME_NAME)
                .addStatement("this.\$L = \$L", FEATURE_VERSION_NAME, FEATURE_VERSION_NAME)
                .build()
        )
        .build()

    override val typeSpec: TypeSpec = TypeSpec.classBuilder(TypeNames.DEVICE_FEATURE_CONDITION)
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        .addFields(listOf(
            featureNameSpec,
            featureVersionSpec
        ))
        .addMethods(listOf(
            constructorSpec,
            featureNameSpec.asGetter(),
            featureVersionSpec.asGetter()
        ))
        .build()


    companion object {
        private const val FEATURE_NAME_NAME = "featureName"
        private const val FEATURE_VERSION_NAME = "featureVersion"
    }
}