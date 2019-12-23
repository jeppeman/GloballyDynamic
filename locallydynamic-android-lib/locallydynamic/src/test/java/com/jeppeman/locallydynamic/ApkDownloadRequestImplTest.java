package com.jeppeman.locallydynamic;

import android.app.DownloadManager;
import android.database.Cursor;
import android.net.Uri;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.play.core.splitinstall.SplitInstallRequest;
import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static com.jeppeman.locallydynamic.serialization.StringUtils.joinToString;

@RunWith(AndroidJUnit4.class)
public class ApkDownloadRequestImplTest {
    private ApkDownloadRequestImpl apkDownloadRequest;
    @Mock
    private Executor mockExecutor;
    @Mock
    private Logger mockLogger;
    @Mock
    private ApkDownloadRequest.StatusListener mockStatusListener;
    @Mock
    private DownloadManager mockDownloadManager;
    @Mock
    private Cursor mockCursor;

    private LocallyDynamicConfigurationDto configuration =
            new LocallyDynamicConfigurationDto(
                    "deviceId",
                    "http://server.url",
                    "username",
                    "password",
                    "applicationid",
                    "variantName",
                    23,
                    5000,
                    new DeviceSpecDto(
                            Lists.newArrayList("a", "b", "c"),
                            Lists.newArrayList("a", "b"),
                            Lists.newArrayList("p"),
                            Lists.newArrayList("extension"),
                            420,
                            23
                    )
            );
    private SplitInstallRequest splitInstallRequest = SplitInstallRequest.newBuilder()
            .addModule("a")
            .addModule("B")
            .addLanguage(Locale.ENGLISH)
            .addLanguage(Locale.CANADA_FRENCH)
            .build();
    private String title = "title";
    private String description = "description";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        apkDownloadRequest = spy(new ApkDownloadRequestImpl(
                spy(ApplicationProvider.getApplicationContext()),
                mockExecutor,
                mockLogger,
                configuration,
                splitInstallRequest,
                title,
                description,
                mockStatusListener,
                mockDownloadManager
        ));

        setupDownloadManagerMock();
    }

    private void setupDownloadManagerMock() {
        when(mockDownloadManager.query(any(DownloadManager.Query.class))).thenReturn(mockCursor);
    }

    @Test
    public void whenDownloadExists_start_shouldReturnImmediately() {
        apkDownloadRequest.downloadId = 20;

        apkDownloadRequest.start();

        verify(mockDownloadManager, never()).enqueue(any(DownloadManager.Request.class));
    }

    @Test
    public void start_shouldEnqueueRequestProperly() {
        final DownloadManager.Request[] currentRequest = new DownloadManager.Request[]{null};
        final Uri[] currentUri = new Uri[]{null};
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                DownloadManager.Request spyRequest =
                        spy((DownloadManager.Request) invocation.callRealMethod());
                currentRequest[0] = spyRequest;
                currentUri[0] = invocation.getArgument(0);
                return spyRequest;
            }
        }).when(apkDownloadRequest).createRequest(any(Uri.class));
        List<String> languages = new ArrayList<String>();
        for (Locale locale : splitInstallRequest.getLanguages()) {
            languages.add(locale.getLanguage());
        }

        apkDownloadRequest.start();

        assertThat(currentUri[0].toString()).contains(configuration.getServerUrl());
        assertThat(currentUri[0].getPathSegments()).contains("download");
        assertThat(currentUri[0].getQueryParameter("device-id")).isEqualTo(configuration.getDeviceId());
        assertThat(currentUri[0].getQueryParameter("variant")).isEqualTo(configuration.getVariantName());
        assertThat(currentUri[0].getQueryParameter("version")).isEqualTo(String.valueOf(configuration.getVersionCode()));
        assertThat(currentUri[0].getQueryParameter("application-id")).isEqualTo(configuration.getApplicationId());
        assertThat(currentUri[0].getQueryParameter("throttle")).isEqualTo(String.valueOf(configuration.getThrottleDownloadBy()));
        assertThat(currentUri[0].getQueryParameter("features")).isEqualTo(joinToString(splitInstallRequest.getModuleNames(), ","));
        assertThat(currentUri[0].getQueryParameter("languages")).isEqualTo(joinToString(languages, ","));
        verify(currentRequest[0]).addRequestHeader(
                "Authorization",
                "Basic " + StringUtils.toBase64(configuration.getUsername()
                        + ":" + configuration.getPassword())
        );
        verify(currentRequest[0]).setTitle(title);
        verify(currentRequest[0]).setDescription(description);
        verify(currentRequest[0]).setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
        verify(currentRequest[0]).setAllowedOverRoaming(true);
        verify(currentRequest[0]).setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE | DownloadManager.Request.NETWORK_WIFI);
        verify(currentRequest[0]).setMimeType("application/zip");
        verify(currentRequest[0]).setAllowedOverMetered(true);
        verify(mockDownloadManager).enqueue(currentRequest[0]);
    }
}