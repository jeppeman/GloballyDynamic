package com.jeppeman.globallydynamic.globalsplitinstall;

import com.jeppeman.globallydynamic.generated.GloballyDynamicBuildConfig;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

//@RunWith(AndroidJUnit4.class)
public class GloballyDynamicConfigurationRepositoryImplTest {
    private GloballyDynamicConfigurationRepositoryImpl globallyDynamicConfigurationRepository;
    @Mock
    GLExtensionsExtractor mockGLExtensionsExtractor;
    @Mock
    Logger mockLogger;
    @Captor
    ArgumentCaptor<DeviceSpecDto> deviceSpecDtoArgumentCaptor;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        globallyDynamicConfigurationRepository = new GloballyDynamicConfigurationRepositoryImpl(
                ApplicationProvider.getApplicationContext(),
                mockGLExtensionsExtractor,
                new GloballyDynamicBuildConfig(),
                mockLogger
        );
    }
}