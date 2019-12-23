package com.jeppeman.locallydynamic;

import android.content.Context;
import android.content.pm.FeatureInfo;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.os.ConfigurationCompat;
import androidx.core.os.LocaleListCompat;
import androidx.core.util.Pair;

import com.jeppeman.locallydynamic.generated.LocallyDynamicBuildConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;


class LocallyDynamicConfigurationRepositoryFactory {
    static LocallyDynamicConfigurationRepository create(
            @NonNull Context context,
            @NonNull GLExtensionsExtractor glExtensionsExtractor,
            @NonNull LocallyDynamicBuildConfig locallyDynamicBuildConfig,
            @NonNull LocallyDynamicApi locallyDynamicApi,
            @NonNull Logger logger) {
        return new LocallyDynamicConfigurationRepositoryImpl(
                context,
                glExtensionsExtractor,
                locallyDynamicBuildConfig,
                locallyDynamicApi,
                logger
        );
    }
}

interface LocallyDynamicConfigurationRepository {
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    Result<LocallyDynamicConfigurationDto> getConfiguration();
}

class LocallyDynamicConfigurationRepositoryImpl implements LocallyDynamicConfigurationRepository {
    private final Context context;
    private final GLExtensionsExtractor glExtensionsExtractor;
    private final LocallyDynamicBuildConfig locallyDynamicBuildConfig;
    private final LocallyDynamicApi locallyDynamicApi;
    private final Logger logger;
    @Nullable
    private DeviceSpecDto deviceSpecDto;

    LocallyDynamicConfigurationRepositoryImpl(
            @NonNull Context context,
            @NonNull GLExtensionsExtractor glExtensionsExtractor,
            @NonNull LocallyDynamicBuildConfig locallyDynamicBuildConfig,
            @NonNull LocallyDynamicApi locallyDynamicApi,
            @NonNull Logger logger) {
        this.context = context;
        this.glExtensionsExtractor = glExtensionsExtractor;
        this.locallyDynamicBuildConfig = locallyDynamicBuildConfig;
        this.locallyDynamicApi = locallyDynamicApi;
        this.logger = logger;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private Result<DeviceSpecDto> getDeviceSpec() {
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

    private Result<Pair<DeviceSpecDto, String>> getDeviceId(
            final @NonNull DeviceSpecDto deviceSpecDto) {
        return locallyDynamicApi.registerDevice(deviceSpecDto).map(
                new Result.Mapper<String, Pair<DeviceSpecDto, String>>() {
                    @Override
                    public Pair<DeviceSpecDto, String> map(String deviceId) {
                        return new Pair<DeviceSpecDto, String>(deviceSpecDto, deviceId);
                    }
                });
    }

    private LocallyDynamicConfigurationDto getConfiguration(
            @NonNull Pair<DeviceSpecDto, String> deviceSpecDtoStringPair) {
        DeviceSpecDto deviceSpecDto = deviceSpecDtoStringPair.first;
        String deviceId = deviceSpecDtoStringPair.second;
        LocallyDynamicConfigurationDto locallyDynamicConfigurationDto =
                new LocallyDynamicConfigurationDto(
                        deviceId,
                        locallyDynamicBuildConfig.getServerUrl(),
                        locallyDynamicBuildConfig.getUsername(),
                        locallyDynamicBuildConfig.getPassword(),
                        locallyDynamicBuildConfig.getApplicationId(),
                        locallyDynamicBuildConfig.getVariantName(),
                        locallyDynamicBuildConfig.getVersionCode(),
                        locallyDynamicBuildConfig.getThrottleDownloadBy(),
                        deviceSpecDto
                );

        logger.i("device configuration: " + locallyDynamicConfigurationDto);

        return locallyDynamicConfigurationDto;
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public Result<LocallyDynamicConfigurationDto> getConfiguration() {
        Result<DeviceSpecDto> deviceSpecResult;
        if (deviceSpecDto != null) {
            deviceSpecResult = Result.of(deviceSpecDto);
        } else {
            deviceSpecResult = getDeviceSpec();
        }

        return deviceSpecResult
                .flatMap(new Result.Mapper<DeviceSpecDto, Result<Pair<DeviceSpecDto, String>>>() {
                    @Override
                    public Result<Pair<DeviceSpecDto, String>> map(DeviceSpecDto deviceSpecDto) {
                        return getDeviceId(deviceSpecDto);
                    }
                })
                .map(new Result.Mapper<Pair<DeviceSpecDto, String>, LocallyDynamicConfigurationDto>() {
                    @Override
                    public LocallyDynamicConfigurationDto map(Pair<DeviceSpecDto, String> pair) {
                        return getConfiguration(pair);
                    }
                });
    }
}