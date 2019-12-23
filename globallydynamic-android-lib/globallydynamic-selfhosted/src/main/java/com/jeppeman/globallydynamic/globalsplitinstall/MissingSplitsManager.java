package com.jeppeman.globallydynamic.globalsplitinstall;

import com.jeppeman.globallydynamic.generated.DeviceFeatureCondition;
import com.jeppeman.globallydynamic.generated.GloballyDynamicBuildConfig;
import com.jeppeman.globallydynamic.generated.ModuleConditions;
import com.jeppeman.globallydynamic.generated.UserCountriesCondition;
import com.jeppeman.globallydynamic.net.ListUtils;

import java.util.Map;
import java.util.Set;

class MissingSplitsManagerFactory {
    static MissingSplitsManager create(
            GloballyDynamicBuildConfig globallyDynamicBuildConfig,
            GloballyDynamicConfigurationRepository globallyDynamicConfigurationRepository,
            Logger logger
    ) {
        return new MissingSplitsManagerImpl(
                globallyDynamicBuildConfig,
                globallyDynamicConfigurationRepository,
                logger
        );
    }
}

interface MissingSplitsManager {
    GlobalSplitInstallRequestInternal getMissingSplitsRequest(Set<String> installedModules);
}

class MissingSplitsManagerImpl implements MissingSplitsManager {
    private final GloballyDynamicBuildConfig globallyDynamicBuildConfig;
    private final GloballyDynamicConfigurationRepository globallyDynamicConfigurationRepository;
    private final Logger logger;

    MissingSplitsManagerImpl(
            GloballyDynamicBuildConfig globallyDynamicBuildConfig,
            GloballyDynamicConfigurationRepository globallyDynamicConfigurationRepository,
            Logger logger
    ) {
        this.globallyDynamicBuildConfig = globallyDynamicBuildConfig;
        this.globallyDynamicConfigurationRepository = globallyDynamicConfigurationRepository;
        this.logger = logger;
    }

    private String moduleConditionsString(ModuleConditions moduleConditions) {
        StringBuilder stringBuilder = new StringBuilder("ModuleCondition(minSdk=")
                .append(moduleConditions.getMinSdkCondition())
                .append(", ")
                .append("deviceFeatures=[");

        for (DeviceFeatureCondition deviceFeatureCondition
                : moduleConditions.getDeviceFeatureConditions()) {
            stringBuilder.append("name=");
            stringBuilder.append(deviceFeatureCondition.getFeatureName())
                    .append(", version=")
                    .append(deviceFeatureCondition.getFeatureVersion());
        }
        stringBuilder.append("], ")
                .append("userCountries={exclude=")
                .append(moduleConditions.getUserCountriesCondition().getExclude())
                .append(", countries=[");

        for (int i = 0; i < moduleConditions.getUserCountriesCondition().getCountries().length; i++) {
            String country = moduleConditions.getUserCountriesCondition().getCountries()[i];
            stringBuilder.append(country);
            if (i < moduleConditions.getUserCountriesCondition().getCountries().length - 1) {
                stringBuilder.append(", ");
            }
        }
        stringBuilder.append("]})");

        return stringBuilder.toString();
    }

    private boolean hasRequiredSdkVersion(
            int minSdk,
            DeviceSpecDto deviceSpecDto
    ) {
        return deviceSpecDto.getSdkVersion() >= minSdk;
    }

    private boolean hasRequiredFeaturesInstalled(
            DeviceFeatureCondition[] deviceFeatureConditions,
            DeviceSpecDto deviceSpecDto
    ) {
        for (DeviceFeatureCondition condition : deviceFeatureConditions) {
            if (!deviceSpecDto.getDeviceFeatures().contains(condition.getFeatureName())) {
                return false;
            }
        }

        return true;
    }

    private boolean hasSomeRequiredCountryInstalled(
            UserCountriesCondition userCountriesCondition,
            DeviceSpecDto deviceSpecDto
    ) {
        for (String country : userCountriesCondition.getCountries()) {
            for (String deviceLocale : deviceSpecDto.getSupportedLocales()) {
                String deviceCountry = deviceLocale.split("-")[1];
                if (userCountriesCondition.getExclude() && deviceCountry.equals(country)) {
                    return false;
                }
                if (!userCountriesCondition.getExclude() && deviceCountry.equals(country)) {
                    return true;
                }
            }
        }

        return userCountriesCondition.getExclude();
    }

    private boolean moduleConditionsSatisfied(ModuleConditions moduleConditions, DeviceSpecDto deviceSpec) {
        return !hasRequiredSdkVersion(moduleConditions.getMinSdkCondition(), deviceSpec)
                || !hasRequiredFeaturesInstalled(moduleConditions.getDeviceFeatureConditions(), deviceSpec)
                || !hasSomeRequiredCountryInstalled(moduleConditions.getUserCountriesCondition(), deviceSpec);
    }

    @Override
    public GlobalSplitInstallRequestInternal getMissingSplitsRequest(Set<String> installedModules) {
        GlobalSplitInstallRequestInternal.Builder requestBuilder =
                GlobalSplitInstallRequestInternal.newBuilder();
        logger.d("Requesting install of missing splits, currently installed modules: "
                + ListUtils.toString(installedModules)
                + ", all install time modues: "
                + ListUtils.toString(globallyDynamicBuildConfig.getInstallTimeFeatures().keySet()));
        for (Map.Entry<String, ModuleConditions> entry
                : globallyDynamicBuildConfig.getInstallTimeFeatures().entrySet()) {
            String moduleName = entry.getKey();
            if (installedModules.contains(moduleName)) {
                // Module is already installed
                logger.d("Module already installed, skipping: " + moduleName);
                continue;
            }

            ModuleConditions moduleConditions = entry.getValue();
            Result<DeviceSpecDto> deviceSpecResult = globallyDynamicConfigurationRepository.getDeviceSpec();
            if (moduleConditions != null && deviceSpecResult instanceof Result.Success) {
                DeviceSpecDto deviceSpec = ((Result.Success<DeviceSpecDto>) deviceSpecResult).data;
                if (!moduleConditionsSatisfied(moduleConditions, deviceSpec)) {
                    // Module conditions not satisfied
                    logger.d("Module condition not satisfied, skipping: "
                            + moduleName + ", " + moduleConditionsString(moduleConditions));
                    continue;
                }
            }

            requestBuilder.addModule(moduleName);
        }

        boolean hasBaseModules = false;
        for (String module : installedModules) {
            if (module.startsWith("base")) {
                hasBaseModules = true;
                break;
            }
        }

        if (!hasBaseModules) {
            requestBuilder.shouldIncludeMissingSplits(true);
        }

        GlobalSplitInstallRequestInternal request = requestBuilder.build();

        logger.d("Missing features to install: "
                + ListUtils.toString(request.getModuleNames())
                + ", will install base splits: "
                + request.shouldIncludeMissingSplits());

        return request;
    }
}
