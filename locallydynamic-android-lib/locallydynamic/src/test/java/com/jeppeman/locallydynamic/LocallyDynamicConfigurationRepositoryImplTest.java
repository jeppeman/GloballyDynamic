package com.jeppeman.locallydynamic;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.jeppeman.locallydynamic.generated.LocallyDynamicBuildConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class LocallyDynamicConfigurationRepositoryImplTest {
    private LocallyDynamicConfigurationRepositoryImpl locallyDynamicConfigurationRepository;
    @Mock
    GLExtensionsExtractor mockGLExtensionsExtractor;
    @Mock
    LocallyDynamicApi mockLocallyDynamicApi;
    @Mock
    Logger mockLogger;
    @Captor
    ArgumentCaptor<DeviceSpecDto> deviceSpecDtoArgumentCaptor;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        locallyDynamicConfigurationRepository = new LocallyDynamicConfigurationRepositoryImpl(
                ApplicationProvider.getApplicationContext(),
                mockGLExtensionsExtractor,
                new LocallyDynamicBuildConfig(),
                mockLocallyDynamicApi,
                mockLogger
        );
    }

    @Test
    public void whenApiSucceeds_getConfiguration_shouldReturnSuccess() {
        final DeviceSpecDto[] deviceSpecDtos = new DeviceSpecDto[]{null};
        when(mockLocallyDynamicApi.registerDevice(deviceSpecDtoArgumentCaptor.capture())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                DeviceSpecDto deviceSpecDto = invocation.getArgument(0);
                deviceSpecDtos[0] = deviceSpecDto;
                return Result.of("deviceId");
            }
        });

        Result<LocallyDynamicConfigurationDto> result = locallyDynamicConfigurationRepository.getConfiguration();

        assertThat(((Result.Success<LocallyDynamicConfigurationDto>)result).data.getDeviceId()).isEqualTo("deviceId");
        assertThat(((Result.Success<LocallyDynamicConfigurationDto>)result).data.getDeviceSpec()).isEqualTo(deviceSpecDtos[0]);
    }

    @Test
    public void whenApiFails_getConfiguration_shouldReturnFailure() {
        Result<LocallyDynamicConfigurationDto> result = locallyDynamicConfigurationRepository.getConfiguration();

        verify(mockLocallyDynamicApi).registerDevice(any(DeviceSpecDto.class));
        assertThat(result).isInstanceOf(Result.Failure.class);
    }
}