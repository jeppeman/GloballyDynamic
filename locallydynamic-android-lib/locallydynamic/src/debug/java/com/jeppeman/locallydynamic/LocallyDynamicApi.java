package com.jeppeman.locallydynamic;

import androidx.annotation.NonNull;

import com.jeppeman.locallydynamic.generated.LocallyDynamicBuildConfig;
import com.jeppeman.locallydynamic.net.HttpClient;
import com.jeppeman.locallydynamic.net.HttpMethod;
import com.jeppeman.locallydynamic.net.HttpUrl;
import com.jeppeman.locallydynamic.net.Request;
import com.jeppeman.locallydynamic.net.Response;

import java.util.Locale;

class LocallyDynamicApiFactory {
    static LocallyDynamicApi create(
            @NonNull HttpClient httpClient,
            @NonNull LocallyDynamicBuildConfig locallyDynamicBuildConfig) {
        return new LocallyDynamicApiImpl(
                httpClient,
                locallyDynamicBuildConfig
        );
    }
}

interface LocallyDynamicApi {
    Result<String> registerDevice(@NonNull DeviceSpecDto deviceSpecDto);
}

class LocallyDynamicApiImpl implements LocallyDynamicApi {
    private final HttpClient httpClient;
    private final LocallyDynamicBuildConfig locallyDynamicBuildConfig;

    LocallyDynamicApiImpl(
            @NonNull HttpClient httpClient,
            @NonNull LocallyDynamicBuildConfig locallyDynamicBuildConfig
    ) {
        this.httpClient = httpClient;
        this.locallyDynamicBuildConfig = locallyDynamicBuildConfig;
    }

    @Override
    public Result<String> registerDevice(final @NonNull DeviceSpecDto deviceSpecDto) {
        return Result.from(new Result.Action<String>() {
            @Override
            public String run() {
                String authorization = StringUtils.toBase64(String.format(
                        Locale.ENGLISH,
                        "%s:%s",
                        locallyDynamicBuildConfig.getUsername(),
                        locallyDynamicBuildConfig.getPassword()
                ));

                Request request = Request.builder()
                        .url(HttpUrl.parse(locallyDynamicBuildConfig.getServerUrl())
                                .newBuilder()
                                .pathSegments("register")
                                .build())
                        .setMethod(HttpMethod.POST)
                        .setBody(deviceSpecDto)
                        .addHeader("Authorization", "Basic " + authorization)
                        .build();

                Response<String> response = httpClient.executeRequest(request);

                if (response.isSuccessful()) {
                    String body = response.getBody();
                    if (body == null) {
                        throw new IllegalStateException("body == null from " + request.getUrl().url());
                    }
                    return body;
                } else {
                    throw new HttpException(response.getCode(), response.getErrorBody());
                }
            }
        });
    }
}