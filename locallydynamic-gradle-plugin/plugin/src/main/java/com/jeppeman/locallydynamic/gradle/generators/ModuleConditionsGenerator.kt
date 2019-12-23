package com.jeppeman.locallydynamic.gradle.generators

import com.squareup.javapoet.*
import java.io.File
import javax.lang.model.element.Modifier

class ModuleConditionsGenerator (outputFile: File) : Generator(outputFile) {
    private val deviceFeatureConditionsSpec = FieldSpec.builder(
        ArrayTypeName.of(TypeNames.DEVICE_FEATURE_CONDITION), DEVICE_FEATURE_CONDITIONS_NAME)
        .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
        .build()

    private val minSdkConditionSpec = FieldSpec.builder(
        TypeNames.INTEGER, MIN_SDK_CONDITION_NAME)
        .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
        .build()

    private val userCountriesConditionSpec = FieldSpec.builder(
        TypeNames.USER_COUNTRIES_CONDITION, USER_COUNTRIES_CONDITION_NAME)
        .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
        .build()

    private val constructorSpec = MethodSpec.constructorBuilder()
        .addParameters(listOf(
            ParameterSpec.builder(
                ArrayTypeName.of(TypeNames.DEVICE_FEATURE_CONDITION),
                DEVICE_FEATURE_CONDITIONS_NAME
            ).build(),
            ParameterSpec.builder(
                TypeNames.INTEGER,
                MIN_SDK_CONDITION_NAME
            ).build(),
            ParameterSpec.builder(
                TypeNames.USER_COUNTRIES_CONDITION,
                USER_COUNTRIES_CONDITION_NAME
            ).build()
        ))
        .addCode(
            CodeBlock.builder()
                .addStatement("this.\$L = \$L", DEVICE_FEATURE_CONDITIONS_NAME, DEVICE_FEATURE_CONDITIONS_NAME)
                .addStatement("this.\$L = \$L", MIN_SDK_CONDITION_NAME, MIN_SDK_CONDITION_NAME)
                .addStatement("this.\$L = \$L", USER_COUNTRIES_CONDITION_NAME, USER_COUNTRIES_CONDITION_NAME)
                .build()
        )
        .build()

    override val typeSpec: TypeSpec = TypeSpec.classBuilder(TypeNames.MODULE_CONDITIONS)
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        .addFields(listOf(
            deviceFeatureConditionsSpec,
            minSdkConditionSpec,
            userCountriesConditionSpec
        ))
        .addMethods(listOf(
            constructorSpec,
            deviceFeatureConditionsSpec.asGetter(),
            minSdkConditionSpec.asGetter(),
            userCountriesConditionSpec.asGetter()
        ))
        .build()

    companion object {
        private const val DEVICE_FEATURE_CONDITIONS_NAME = "deviceFeatureConditions"
        private const val MIN_SDK_CONDITION_NAME = "minSdkCondition"
        private const val USER_COUNTRIES_CONDITION_NAME = "userCountriesCondition"
    }
}