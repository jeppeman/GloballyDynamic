package com.jeppeman.locallydynamic.generated;

/**
 * This class is solely provided for compilation purposes, it will be rewritten by the
 * gradle plugin when building the bundle
 */
public final class UserCountriesCondition {
  private final boolean exclude;

  private final String[] countries;

  UserCountriesCondition(String[] countries, boolean exclude) {
    this.countries = countries;
    this.exclude = exclude;
  }

  public boolean getExclude() {
    return exclude;
  }

  public String[] getCountries() {
    return countries;
  }
}
