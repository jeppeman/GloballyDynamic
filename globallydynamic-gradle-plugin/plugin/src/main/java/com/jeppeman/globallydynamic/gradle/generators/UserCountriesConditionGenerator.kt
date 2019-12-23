package com.jeppeman.globallydynamic.gradle.generators

import com.squareup.javapoet.*
import java.io.File
import javax.lang.model.element.Modifier

internal class UserCountriesConditionGenerator(outputFile: File) : Generator(outputFile) {
    private val userCountriesSpec = FieldSpec.builder(ArrayTypeName.of(TypeNames.STRING), COUNTRIES_NAME)
        .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
        .build()

    private val excludeSpec = FieldSpec.builder(TypeName.BOOLEAN, EXCLUDE_NAME)
        .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
        .build()

    private val constructorSpec = MethodSpec.constructorBuilder()
        .addParameters(listOf(
            ParameterSpec.builder(
                ArrayTypeName.of(TypeNames.STRING),
                COUNTRIES_NAME
            ).build(),
            ParameterSpec.builder(
                TypeName.BOOLEAN,
                EXCLUDE_NAME
            ).build()
        ))
        .addCode(
            CodeBlock.builder()
                .addStatement("this.\$L = \$L", COUNTRIES_NAME, COUNTRIES_NAME)
                .addStatement("this.\$L = \$L", EXCLUDE_NAME, EXCLUDE_NAME)
                .build()
        )
        .build()

    override val typeSpec: TypeSpec = TypeSpec.classBuilder(TypeNames.USER_COUNTRIES_CONDITION)
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        .addFields(listOf(
            excludeSpec,
            userCountriesSpec
        ))
        .addMethods(listOf(
            constructorSpec,
            excludeSpec.asGetter(),
            userCountriesSpec.asGetter()
        ))
        .build()


    companion object {
        private const val COUNTRIES_NAME = "countries"
        private const val EXCLUDE_NAME = "exclude"
    }
}