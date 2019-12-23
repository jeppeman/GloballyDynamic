package com.jeppeman.globallydynamic.gradle.generators

import com.squareup.javapoet.ClassName

internal object TypeNames {
    const val PACKAGE_NAME: String = "com.jeppeman.globallydynamic.generated"
    val STRING: ClassName = ClassName.get(String::class.java)
    val MAP: ClassName = ClassName.get(Map::class.java)
    val HASH_MAP: ClassName = ClassName.get(HashMap::class.java)
    val GLOBALLY_DYNAMIC_BUILD_CONFIG: ClassName = ClassName.get(PACKAGE_NAME, "GloballyDynamicBuildConfig")
    val MODULE_CONDITIONS: ClassName = ClassName.get(PACKAGE_NAME, "ModuleConditions")
    val DEVICE_FEATURE_CONDITION: ClassName = ClassName.get(PACKAGE_NAME, "DeviceFeatureCondition")
    val USER_COUNTRIES_CONDITION: ClassName = ClassName.get(PACKAGE_NAME, "UserCountriesCondition")
    val INTEGER: ClassName = ClassName.get(Int::class.javaObjectType)
}