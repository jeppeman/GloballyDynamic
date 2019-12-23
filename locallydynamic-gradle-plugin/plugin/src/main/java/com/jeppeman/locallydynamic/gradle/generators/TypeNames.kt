package com.jeppeman.locallydynamic.gradle.generators

import com.squareup.javapoet.ClassName

object TypeNames {
    const val PACKAGE_NAME: String = "com.jeppeman.locallydynamic.generated"
    val STRING: ClassName = ClassName.get(String::class.java)
    val MAP: ClassName = ClassName.get(Map::class.java)
    val HASH_MAP: ClassName = ClassName.get(HashMap::class.java)
    val LOCALLY_DYNAMIC_BUILD_CONFIG: ClassName = ClassName.get(PACKAGE_NAME, "LocallyDynamicBuildConfig")
    val MODULE_CONDITIONS: ClassName = ClassName.get(PACKAGE_NAME, "ModuleConditions")
    val DEVICE_FEATURE_CONDITION: ClassName = ClassName.get(PACKAGE_NAME, "DeviceFeatureCondition")
    val USER_COUNTRIES_CONDITION: ClassName = ClassName.get(PACKAGE_NAME, "UserCountriesCondition")
    val INTEGER: ClassName = ClassName.get(Int::class.javaObjectType)
}