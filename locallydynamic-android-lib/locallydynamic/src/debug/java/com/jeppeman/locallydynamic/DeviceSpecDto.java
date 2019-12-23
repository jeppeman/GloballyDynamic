package com.jeppeman.locallydynamic;

import androidx.annotation.NonNull;

import com.jeppeman.locallydynamic.net.ListUtils;
import com.jeppeman.locallydynamic.serialization.annotations.JsonDeserialize;
import com.jeppeman.locallydynamic.serialization.annotations.JsonSerialize;

import java.util.List;
import java.util.Locale;

class DeviceSpecDto {
    @JsonSerialize("supportedAbis")
    private List<String> supportedAbis;
    @JsonSerialize("supportedLocales")
    private List<String> supportedLocales;
    @JsonSerialize("deviceFeatures")
    private List<String> deviceFeatures;
    @JsonSerialize("glExtensions")
    private List<String> glExtensions;
    @JsonSerialize("screenDensity")
    private int screenDensity;
    @JsonSerialize("sdkVersion")
    private int sdkVersion;

    DeviceSpecDto(
            @JsonDeserialize("supportedAbis") List<String> supportedAbis,
            @JsonDeserialize("supportedLocales") List<String> supportedLocales,
            @JsonDeserialize("deviceFeatures") List<String> deviceFeatures,
            @JsonDeserialize("glExtensions") List<String> glExtensions,
            @JsonDeserialize("screenDensity") int screenDensity,
            @JsonDeserialize("sdkVersion") int sdkVersion) {
        this.supportedAbis = supportedAbis;
        this.supportedLocales = supportedLocales;
        this.deviceFeatures = deviceFeatures;
        this.glExtensions = glExtensions;
        this.screenDensity = screenDensity;
        this.sdkVersion = sdkVersion;
    }

    public List<String> getSupportedAbis() {
        return supportedAbis;
    }

    public List<String> getSupportedLocales() {
        return supportedLocales;
    }

    public List<String> getDeviceFeatures() {
        return deviceFeatures;
    }

    public List<String> getGlExtensions() {
        return glExtensions;
    }

    public int getScreenDensity() {
        return screenDensity;
    }

    public int getSdkVersion() {
        return sdkVersion;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format(
                Locale.ENGLISH,
                "%s(supportedAbis=%s, supportedLocales=%s, deviceFeatures=%s, glExtensions=%s, " +
                        "screenDensity=%d, sdkVersion=%d)",
                getClass().getSimpleName(),
                ListUtils.toString(supportedAbis),
                ListUtils.toString(supportedLocales),
                ListUtils.toString(deviceFeatures),
                ListUtils.toString(glExtensions),
                screenDensity,
                sdkVersion
        );
    }
}
