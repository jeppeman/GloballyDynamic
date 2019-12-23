package com.jeppeman.globallydynamic.globalsplitinstall;

import com.jeppeman.globallydynamic.serialization.annotations.JsonDeserialize;
import com.jeppeman.globallydynamic.serialization.annotations.JsonSerialize;

import java.util.Locale;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

@Keep
class GloballyDynamicConfigurationDto {
    @JsonSerialize("serverUrl")
    private String serverUrl;
    @JsonSerialize("applicationId")
    private String applicationId;
    @JsonSerialize("variantName")
    private String variantName;
    @JsonSerialize("versionCode")
    private int versionCode;
    @JsonSerialize("throttleDownloadBy")
    private long throttleDownloadBy;
    @JsonSerialize("deviceSpec")
    private DeviceSpecDto deviceSpec;

    GloballyDynamicConfigurationDto(
            @JsonDeserialize("serverUrl") String serverUrl,
            @JsonDeserialize("applicationId") String applicationId,
            @JsonDeserialize("variantName") String variantName,
            @JsonDeserialize("versionCode") int versionCode,
            @JsonDeserialize("throttleDownloadBy") long throttleDownloadBy,
            @JsonDeserialize("deviceSpec") DeviceSpecDto deviceSpec) {
        this.serverUrl = serverUrl;
        this.applicationId = applicationId;
        this.variantName = variantName;
        this.versionCode = versionCode;
        this.throttleDownloadBy = throttleDownloadBy;
        this.deviceSpec = deviceSpec;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public String getVariantName() {
        return variantName;
    }

    public int getVersionCode() {
        return versionCode;
    }

    public long getThrottleDownloadBy() {
        return throttleDownloadBy;
    }

    public DeviceSpecDto getDeviceSpec() {
        return deviceSpec;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format(
                Locale.ENGLISH,
                "%s(serverUrl=%s, applicationId=%s, "
                        + "variantName=%s, versionCode=%d, throttleDownloadBy=%d, deviceSpec=%s)",
                getClass().getSimpleName(),
                serverUrl,
                applicationId,
                variantName,
                versionCode,
                throttleDownloadBy,
                deviceSpec.toString()
        );
    }
}