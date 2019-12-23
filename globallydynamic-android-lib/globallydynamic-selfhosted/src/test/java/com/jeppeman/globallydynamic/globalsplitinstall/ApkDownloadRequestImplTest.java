package com.jeppeman.globallydynamic.globalsplitinstall;

import android.os.Bundle;

import com.google.common.collect.Lists;
import com.jeppeman.globallydynamic.net.HttpClient;
import com.jeppeman.globallydynamic.net.HttpUrl;
import com.jeppeman.globallydynamic.net.Request;
import com.jeppeman.globallydynamic.net.Response;
import com.jeppeman.globallydynamic.net.StreamUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.util.io.IOUtil;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.util.TempDirectory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static com.google.common.truth.Truth.assertThat;
import static com.jeppeman.globallydynamic.serialization.StringUtils.joinToString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    private HttpClient mockHttpClient;
    @Mock
    private SignatureProvider mockSignatureProvider;

    private GloballyDynamicConfigurationDto configuration =
            new GloballyDynamicConfigurationDto(
                    "http://server.url",
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
    private GlobalSplitInstallRequestInternal splitInstallRequest = GlobalSplitInstallRequestInternal.newBuilder()
            .addModule("a")
            .addModule("B")
            .addLanguage(Locale.ENGLISH)
            .addLanguage(Locale.CANADA_FRENCH)
            .build();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        apkDownloadRequest = spy(new ApkDownloadRequestImpl(
                spy(ApplicationProvider.getApplicationContext()),
                mockExecutor,
                mockLogger,
                mockSignatureProvider,
                configuration,
                splitInstallRequest,
                mockStatusListener,
                mockHttpClient
        ));

        setupExecutorMock();
    }

    private void setupExecutorMock() {
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                Executor.Callbacks<Object> callbacks =
                        invocation.getArgument(0);
                try {
                    callbacks.onComplete(callbacks.execute());
                } catch (Exception exception) {
                    callbacks.onError(exception);
                }
                return null;
            }
        }).when(mockExecutor).execute(
                ArgumentCaptor.<Executor.Callbacks, Executor.Callbacks>forClass(
                        Executor.Callbacks.class).capture());

        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                Executor.Callbacks<Object> callbacks =
                        invocation.getArgument(0);
                try {
                    callbacks.onComplete(callbacks.execute());
                } catch (Exception exception) {
                    callbacks.onError(exception);
                }
                return null;
            }
        }).when(mockExecutor).executeForeground(
                ArgumentCaptor.<Executor.Callbacks, Executor.Callbacks>forClass(
                        Executor.Callbacks.class).capture());
    }

    @Test
    public void whenDownloadExists_start_shouldReturnImmediately() {
        apkDownloadRequest.downloadId = 20;

        apkDownloadRequest.start();

        verify(mockHttpClient, never()).downloadFile(any(Request.class), any(File.class), any(HttpClient.DownloadCallbacks.class));
    }

    @Test
    public void start_shouldEnqueueRequestProperly() {
        when(mockSignatureProvider.getCertificateFingerprint()).thenReturn("fingerprint");
        final Request[] request = new Request[]{null};
        final HttpUrl[] currentUri = new HttpUrl[]{null};
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Request spyRequest =
                        spy((Request) invocation.getArgument(0));
                request[0] = spyRequest;
                currentUri[0] = spyRequest.getUrl();
                return 1;
            }
        }).when(mockHttpClient).downloadFile(any(Request.class), any(File.class), any(HttpClient.DownloadCallbacks.class));
        List<String> languages = new ArrayList<String>();
        for (Locale locale : splitInstallRequest.getLanguages()) {
            languages.add(locale.getLanguage());
        }

        apkDownloadRequest.start();

        assertThat(currentUri[0].url()).contains(configuration.getServerUrl());
        assertThat(currentUri[0].pathSegments()).contains("download");
        assertThat(currentUri[0].queryParams().get("variant")).isEqualTo(configuration.getVariantName());
        assertThat(currentUri[0].queryParams().get("version")).isEqualTo(String.valueOf(configuration.getVersionCode()));
        assertThat(currentUri[0].queryParams().get("application-id")).isEqualTo(configuration.getApplicationId());
        assertThat(currentUri[0].queryParams().get("throttle")).isEqualTo(String.valueOf(configuration.getThrottleDownloadBy()));
        assertThat(currentUri[0].queryParams().get("features")).isEqualTo(joinToString(splitInstallRequest.getModuleNames(), ","));
        assertThat(currentUri[0].queryParams().get("languages")).isEqualTo(joinToString(languages, ","));
        assertThat(apkDownloadRequest.downloadId).isEqualTo(1);
    }

    @Test
    public void whenActionIsUserCanceled_onReceiveResult_shouldCancel() {
        Bundle args = new Bundle();
        args.putBoolean(InstallService.ACTION_CANCELED, true);

        apkDownloadRequest.onReceiveResult(0, args);

        verify(mockHttpClient).cancelDownload(apkDownloadRequest.downloadId);
    }

    @Test
    public void whenActionIsNotIsUserCanceled_onReceiveResult_shouldNotCancel() {
        apkDownloadRequest.onReceiveResult(0, new Bundle());

        verify(mockHttpClient, never()).cancelDownload(anyInt());
    }

    @Test
    public void whenSuccessful_onResult_shouldExtractApksAndEmitSuccess() throws IOException {
        FileOutputStream fos = new FileOutputStream(apkDownloadRequest.downloadedApks);
        fos.write(StreamUtils.readAllBytes(getClass().getClassLoader().getResourceAsStream("dummy.aab")));
        Response response = Response.builder().setCode(200).build();
        assertThat(apkDownloadRequest.downloadedSplitsDir.listFiles()).isEmpty();

        apkDownloadRequest.onResponse(response, 100, 100);

        assertThat(apkDownloadRequest.downloadedSplitsDir.listFiles()).isNotEmpty();
        verify(mockStatusListener).onUpdate(isA(ApkDownloadRequest.Status.Successful.class));
    }

    @Test
    public void whenFailed_onResult_shouldEmitFailure() {
        Response response = Response.builder().setCode(500).build();

        apkDownloadRequest.onResponse(response, 100, 100);

        verify(mockStatusListener).onUpdate(isA(ApkDownloadRequest.Status.Failed.class));
    }

    @Test
    public void onStartDownload_shouldEmitEnqueuedStatus() {
        apkDownloadRequest.onStartDownload(50);

        ArgumentCaptor<ApkDownloadRequest.Status.Enqueued> captor =
                ArgumentCaptor.forClass(ApkDownloadRequest.Status.Enqueued.class);
        verify(mockStatusListener).onUpdate(captor.capture());
        assertThat(captor.getValue().id).isEqualTo(apkDownloadRequest.downloadId);
    }

    @Test
    public void onProgress_shouldEmitRunningStatus() {
        apkDownloadRequest.onProgress(50, 100);

        ArgumentCaptor<ApkDownloadRequest.Status.Running> captor =
                ArgumentCaptor.forClass(ApkDownloadRequest.Status.Running.class);
        verify(mockStatusListener).onUpdate(captor.capture());
        assertThat(captor.getValue().bytesDownloaded).isEqualTo(50);
        assertThat(captor.getValue().totalBytes).isEqualTo(100);
    }
}