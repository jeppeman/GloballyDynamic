package com.jeppeman.globallydynamic.generated;

/**
 * This class is solely provided for compilation purposes, it will be rewritten by the
 * gradle plugin when building the bundle
 */
public final class DeviceFeatureCondition {
  private final String featureName;

  private final Integer featureVersion;

  DeviceFeatureCondition(String featureName, Integer featureVersion) {
    this.featureName = featureName;
    this.featureVersion = featureVersion;
  }

  public String getFeatureName() {
    return featureName;
  }

  public Integer getFeatureVersion() {
    return featureVersion;
  }
}
