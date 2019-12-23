package com.jeppeman.locallydynamic;

import androidx.annotation.NonNull;

import com.jeppeman.locallydynamic.serialization.annotations.JsonDeserialize;
import com.jeppeman.locallydynamic.serialization.annotations.JsonSerialize;

import java.util.Locale;

class LocallyDynamicConfigurationDto {
    @JsonSerialize("deviceId")
    private String deviceId;
    @JsonSerialize("serverUrl")
    private String serverUrl;
    @JsonSerialize("username")
    private String username;
    @JsonSerialize("password")
    private String password;
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

    LocallyDynamicConfigurationDto(
            @JsonDeserialize("deviceId") String deviceId,
            @JsonDeserialize("serverUrl") String serverUrl,
            @JsonDeserialize("username") String username,
            @JsonDeserialize("password") String password,
            @JsonDeserialize("applicationId") String applicationId,
            @JsonDeserialize("variantName") String variantName,
            @JsonDeserialize("versionCode") int versionCode,
            @JsonDeserialize("throttleDownloadBy") long throttleDownloadBy,
            @JsonDeserialize("deviceSpec") DeviceSpecDto deviceSpec) {
        this.deviceId = deviceId;
        this.serverUrl = serverUrl;
        this.username = username;
        this.password = password;
        this.applicationId = applicationId;
        this.variantName = variantName;
        this.versionCode = versionCode;
        this.throttleDownloadBy = throttleDownloadBy;
        this.deviceSpec = deviceSpec;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
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
                "%s(deviceId=%s, serverUrl=%s, username=%s, password=%s, applicationId=%s, "
                        + "variantName=%s, versionCode=%d, throttleDownloadBy=%d, deviceSpec=%s)",
                getClass().getSimpleName(),
                deviceId,
                serverUrl,
                username,
                password,
                applicationId,
                variantName,
                versionCode,
                throttleDownloadBy,
                deviceSpec.toString()
        );
    }
}