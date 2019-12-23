package com.jeppeman.locallydynamic;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.common.collect.Lists;
import com.jeppeman.locallydynamic.generated.LocallyDynamicBuildConfig;
import com.jeppeman.locallydynamic.net.HttpClient;
import com.jeppeman.locallydynamic.net.HttpMethod;
import com.jeppeman.locallydynamic.net.Request;
import com.jeppeman.locallydynamic.net.Response;
import com.jeppeman.locallydynamic.net.StreamUtils;
import com.jeppeman.locallydynamic.serialization.JsonSerializer;
import com.jeppeman.locallydynamic.serialization.JsonSerializerFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Locale;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class LocallyDynamicApiImplTest {
    private static final String URL = "http://url.com";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";

    private LocallyDynamicApiImpl locallyDynamicApi;
    @Mock
    private HttpClient mockHttpClient;
    @Mock
    private LocallyDynamicBuildConfig mockLocallyDynamicBuildConfig;
    @Captor
    private ArgumentCaptor<Request> requestArgumentCaptor;

    private DeviceSpecDto deviceSpecDto = new DeviceSpecDto(
            Lists.newArrayList("a"),
            Lists.newArrayList("a"),
            Lists.newArrayList("a"),
            Lists.newArrayList("a"),
            420,
            23
    );

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        locallyDynamicApi = new LocallyDynamicApiImpl(
                mockHttpClient,
                mockLocallyDynamicBuildConfig
        );

        doReturn(URL).when(mockLocallyDynamicBuildConfig).getServerUrl();
        doReturn(USERNAME).when(mockLocallyDynamicBuildConfig).getUsername();
        doReturn(PASSWORD).when(mockLocallyDynamicBuildConfig).getPassword();
    }

    @Test
    public void registerDevice_shouldFormatProperRequest() {
        when(mockHttpClient.executeRequest(requestArgumentCaptor.capture())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                Request request = invocation.getArgument(0);
                assertThat(request.getUrl().url()).isEqualTo(URL + "/register");
                assertThat(request.getHeaders().get("Authorization").get(0)).isEqualTo(
                        "Basic " + StringUtils.toBase64(String.format(
                                Locale.ENGLISH,
                                "%s:%s",
                                USERNAME,
                                PASSWORD
                        ))
                );
                assertThat(request.getMethod()).isEqualTo(HttpMethod.POST);
                assertThat(StreamUtils.readString(request.getBody())).isEqualTo(JsonSerializerFactory.create().serialize(deviceSpecDto));
                return null;
            }
        });
    }

    @Test
    public void whenRequestSucceeds_registerDevice_shouldReturnDeviceId() {
        final String deviceId = "deviceId";
        when(mockHttpClient.executeRequest(requestArgumentCaptor.capture())).thenReturn(
                Response.<String>builder()
                        .setBody(deviceId)
                        .setCode(200)
                        .build());

        Result<String> result = locallyDynamicApi.registerDevice(deviceSpecDto);

        assertThat(((Result.Success<String>) result).data).isEqualTo(deviceId);
    }

    @Test
    public void whenRequestSucceedsButHasNoBody_registerDevice_shouldFailWithIllegalState() {
        when(mockHttpClient.executeRequest(requestArgumentCaptor.capture())).thenReturn(
                Response.<String>builder()
                        .setCode(200)
                        .build());

        Result<String> result = locallyDynamicApi.registerDevice(deviceSpecDto);

        assertThat(((Result.Failure) result).exception).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void whenRequestFails_registerDevice_shouldFailWithHttpException() {
        String errorBody = "errorBody";
        when(mockHttpClient.executeRequest(requestArgumentCaptor.capture())).thenReturn(
                Response.<String>builder()
                        .setCode(404)
                        .setErrorBody(errorBody)
                        .build());

        Result<String> result = locallyDynamicApi.registerDevice(deviceSpecDto);
        HttpException httpException = (HttpException) ((Result.Failure) result).exception;

        assertThat(httpException.code).isEqualTo(404);
        assertThat(httpException.message).isEqualTo(errorBody);
    }
}