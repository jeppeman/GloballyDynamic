package com.jeppeman.locallydynamic.generated;

/**
 * This class is solely provided for compilation purposes, it will be rewritten by the
 * gradle plugin when building the bundle
 */
public final class ModuleConditions {
  private final DeviceFeatureCondition[] deviceFeatureConditions;

  private final Integer minSdkCondition;

  private final UserCountriesCondition userCountriesCondition;

  ModuleConditions(DeviceFeatureCondition[] deviceFeatureConditions, Integer minSdkCondition,
      UserCountriesCondition userCountriesCondition) {
    this.deviceFeatureConditions = deviceFeatureConditions;
    this.minSdkCondition = minSdkCondition;
    this.userCountriesCondition = userCountriesCondition;
  }

  public DeviceFeatureCondition[] getDeviceFeatureConditions() {
    return deviceFeatureConditions;
  }

  public Integer getMinSdkCondition() {
    return minSdkCondition;
  }

  public UserCountriesCondition getUserCountriesCondition() {
    return userCountriesCondition;
  }
}
