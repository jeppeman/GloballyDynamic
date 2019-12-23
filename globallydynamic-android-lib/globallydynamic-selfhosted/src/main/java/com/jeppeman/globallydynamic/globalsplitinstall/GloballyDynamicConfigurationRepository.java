package com.jeppeman.globallydynamic.globalsplitinstall;

import android.content.Context;
import android.content.pm.FeatureInfo;
import android.os.Build;

import com.jeppeman.globallydynamic.generated.GloballyDynamicBuildConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.os.ConfigurationCompat;
import androidx.core.os.LocaleListCompat;


class GloballyDynamicConfigurationRepositoryFactory {
    static GloballyDynamicConfigurationRepository create(
            @NonNull Context context,
            @NonNull GLExtensionsExtractor glExtensionsExtractor,
            @NonNull GloballyDynamicBuildConfig globallyDynamicBuildConfig,
            @NonNull Logger logger) {
        return new GloballyDynamicConfigurationRepositoryImpl(
                context,
                glExtensionsExtractor,
                globallyDynamicBuildConfig,
                logger
        );
    }
}

interface GloballyDynamicConfigurationRepository {
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    Result<GloballyDynamicConfigurationDto> getConfiguration();

    Result<DeviceSpecDto> getDeviceSpec();
}

class GloballyDynamicConfigurationRepositoryImpl implements GloballyDynamicConfigurationRepository {
    private final Context context;
    private final GLExtensionsExtractor glExtensionsExtractor;
    private final GloballyDynamicBuildConfig globallyDynamicBuildConfig;
    private final Logger logger;
    @Nullable
    private DeviceSpecDto deviceSpecDto;

    GloballyDynamicConfigurationRepositoryImpl(
            @NonNull Context context,
            @NonNull GLExtensionsExtractor glExtensionsExtractor,
            @NonNull GloballyDynamicBuildConfig globallyDynamicBuildConfig,
            @NonNull Logger logger) {
        this.context = context;
        this.glExtensionsExtractor = glExtensionsExtractor;
        this.globallyDynamicBuildConfig = globallyDynamicBuildConfig;
        this.logger = logger;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public Result<DeviceSpecDto> getDeviceSpec() {
        if (deviceSpecDto != null) {
            return Result.of(deviceSpecDto);
        }

        return Result.from(new Result.Action<DeviceSpecDto>() {
            @Override
            public DeviceSpecDto run() {
                FeatureInfo[] systemAvailableFeatures = context.getPackageManager()
                        .getSystemAvailableFeatures();
                List<String> availableFeatures = new ArrayList<String>(
                        systemAvailableFeatures != null
                                ? systemAvailableFeatures.length
                                : 0);

                if (systemAvailableFeatures != null) {
                    for (FeatureInfo featureInfo : systemAvailableFeatures) {
                        availableFeatures.add(StringUtils.fromFeatureInfo(featureInfo));
                    }
                }

                LocaleListCompat locales = ConfigurationCompat.getLocales(
                        context.getResources().getConfiguration());

                List<String> supportedLocales = new ArrayList<String>(locales.size());

                for (int i = 0; i < locales.size(); i++) {
                    Locale locale = locales.get(i);
                    supportedLocales.add(locale.getLanguage() + "-" + locale.getCountry());
                }

                deviceSpecDto = new DeviceSpecDto(
                        Arrays.asList(Build.SUPPORTED_ABIS),
                        supportedLocales,
                        availableFeatures,
                        glExtensionsExtractor.extract(),
                        context.getResources().getConfiguration().densityDpi,
                        Build.VERSION.SDK_INT
                );

                return deviceSpecDto;
            }
        });
    }

    private GloballyDynamicConfigurationDto getConfiguration(@NonNull DeviceSpecDto deviceSpecDto) {
        GloballyDynamicConfigurationDto globallyDynamicConfigurationDto =
                new GloballyDynamicConfigurationDto(
                        globallyDynamicBuildConfig.getServerUrl(),
                        globallyDynamicBuildConfig.getApplicationId(),
                        globallyDynamicBuildConfig.getVariantName(),
                        globallyDynamicBuildConfig.getVersionCode(),
                        globallyDynamicBuildConfig.getThrottleDownloadBy(),
                        deviceSpecDto
                );

        logger.i("device configuration: " + globallyDynamicConfigurationDto);

        return globallyDynamicConfigurationDto;
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public Result<GloballyDynamicConfigurationDto> getConfiguration() {
        return getDeviceSpec()
                .map(new Result.Mapper<DeviceSpecDto, GloballyDynamicConfigurationDto>() {
                    @Override
                    public GloballyDynamicConfigurationDto map(DeviceSpecDto deviceSpecDto) {
                        return getConfiguration(deviceSpecDto);
                    }
                });
    }
}