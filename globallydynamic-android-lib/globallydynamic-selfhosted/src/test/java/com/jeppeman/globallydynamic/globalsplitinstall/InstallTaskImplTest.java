package com.jeppeman.globallydynamic.globalsplitinstall;

import android.content.Context;

import com.google.common.collect.Lists;
import com.jeppeman.globallydynamic.net.HttpClient;
import com.jeppeman.globallydynamic.tasks.GlobalSplitInstallTask;
import com.jeppeman.globallydynamic.tasks.OnGlobalSplitInstallCompleteListener;
import com.jeppeman.globallydynamic.tasks.OnGlobalSplitInstallFailureListener;
import com.jeppeman.globallydynamic.tasks.OnGlobalSplitInstallSuccessListener;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class InstallTaskImplTest {
    private InstallTaskImpl globallyDynamicInstallTask;
    @Mock
    private GloballyDynamicConfigurationRepository mockGloballyDynamicConfigurationRepository;
    @Mock
    private Context mockContext;
    @Mock
    private Executor mockExecutor;
    @Mock
    private Logger mockLogger;
    @Mock
    private SignatureProvider mockSignatureProvider;
    @Mock
    private ApplicationPatcher mockApplicationPatcher;
    @Mock
    private TaskRegistry mockTaskRegistry;
    @Mock
    private HttpClient mockHttpClient;
    private GlobalSplitInstallSessionState currentState;

    private GlobalSplitInstallRequestInternal globalSplitInstallRequest = GlobalSplitInstallRequestInternal.newBuilder()
            .addLanguage(Locale.ENGLISH)
            .addLanguage(Locale.CANADA)
            .addModule("a")
            .addModule("b")
            .build();

    private GloballyDynamicConfigurationDto globallyDynamicConfigurationDto =
            new GloballyDynamicConfigurationDto(
                    "serverUrl",
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

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        globallyDynamicInstallTask = Mockito.spy(new InstallTaskImpl(
                mockGloballyDynamicConfigurationRepository,
                mockContext,
                new InstallListenersProvider() {
                    @Override
                    public List<GlobalSplitInstallUpdatedListener> getListeners() {
                        return Lists.<GlobalSplitInstallUpdatedListener>newArrayList(
                                new GlobalSplitInstallUpdatedListener() {
                                    @Override
                                    public void onStateUpdate(GlobalSplitInstallSessionState state) {
                                        currentState = state;
                                    }
                                });
                    }
                }
                ,
                globalSplitInstallRequest,
                mockExecutor,
                mockLogger,
                mockSignatureProvider,
                mockApplicationPatcher,
                mockTaskRegistry,
                mockHttpClient
        ));

        setupExecutorMock();
    }

    @After
    public void tearDown() {
        Mockito.reset();
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

    private ApkDownloadRequest mockDownloadStatusEmission(final ApkDownloadRequest.Status status) {
        final ApkDownloadRequest mockDownloadRequest = Mockito.mock(ApkDownloadRequest.class);
        Mockito.when(mockGloballyDynamicConfigurationRepository.getConfiguration()).thenReturn(Result.of(globallyDynamicConfigurationDto));
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                ApkDownloadRequest.StatusListener statusListener = invocation.getArgument(1);
                statusListener.onUpdate(status);
                return mockDownloadRequest;
            }
        }).when(globallyDynamicInstallTask).createApkDownloadRequest(
                eq(globallyDynamicConfigurationDto),
                ArgumentCaptor.<ApkDownloadRequest.StatusListener, ApkDownloadRequest.StatusListener>
                        forClass(ApkDownloadRequest.StatusListener.class).capture()
        );

        return mockDownloadRequest;
    }

    private void mockApkInstallerStatusEmission(final ApkInstaller.Status status) {
        mockDownloadStatusEmission(new ApkDownloadRequest.Status.Successful(0, 0, new ArrayList<File>()));
        final ApkInstaller mockApkInstaller = Mockito.mock(ApkInstaller.class);
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                ApkInstaller.StatusListener statusListener = invocation.getArgument(0);
                statusListener.onUpdate(status);
                return mockApkInstaller;
            }
        }).when(globallyDynamicInstallTask).createApkInstaller(
                ArgumentCaptor.<ApkInstaller.StatusListener, ApkInstaller.StatusListener>
                        forClass(ApkInstaller.StatusListener.class).capture()
        );
    }

    @Test
    public void whenConfigurationIsSuccessfullyFetched_start_shouldStartDownload() {
        ApkDownloadRequest mockDownloadRequest = Mockito.mock(ApkDownloadRequest.class);
        Mockito.when(mockGloballyDynamicConfigurationRepository.getConfiguration()).thenReturn(Result.of(globallyDynamicConfigurationDto));
        doReturn(mockDownloadRequest).when(globallyDynamicInstallTask).createApkDownloadRequest(
                eq(globallyDynamicConfigurationDto),
                ArgumentCaptor.<ApkDownloadRequest.StatusListener, ApkDownloadRequest.StatusListener>
                        forClass(ApkDownloadRequest.StatusListener.class).capture()
        );

        globallyDynamicInstallTask.start();

        verify(mockDownloadRequest).start();
    }

    @Test
    public void whenConfigurationFetchFails_start_shouldFailTask() {
        final IllegalStateException illegalStateException = new IllegalStateException("yo");
        Mockito.when(mockGloballyDynamicConfigurationRepository.getConfiguration()).thenReturn(Result.of(illegalStateException));

        globallyDynamicInstallTask.start();

        assertThat((globallyDynamicInstallTask.getException()).getCause()).isEqualTo(illegalStateException);
        assertThat(globallyDynamicInstallTask.isSuccessful()).isFalse();
    }

    @Test
    public void whenConfigurationFetchThrows_start_shouldFailTask() {
        final IllegalStateException illegalStateException = new IllegalStateException("yo");
        final boolean[] calledOnFailure = new boolean[]{false};
        Mockito.when(mockGloballyDynamicConfigurationRepository.getConfiguration()).thenThrow(illegalStateException);
        globallyDynamicInstallTask.addOnFailureListener(new OnGlobalSplitInstallFailureListener() {
            @Override
            public void onFailure(Exception e) {
                calledOnFailure[0] = true;
                assertThat(e.getCause()).isEqualTo(illegalStateException);
            }
        });

        globallyDynamicInstallTask.start();

        assertThat(calledOnFailure[0]).isTrue();
        assertThat(globallyDynamicInstallTask.getException().getCause()).isEqualTo(illegalStateException);
        assertThat(globallyDynamicInstallTask.isSuccessful()).isFalse();
    }

    @Test
    public void whenApkDownloadRequestEmitsPending_getCurrentState_shouldBePending() {
        mockDownloadStatusEmission(new ApkDownloadRequest.Status.Pending(0, 0));

        globallyDynamicInstallTask.start();
        GlobalSplitInstallSessionState state = globallyDynamicInstallTask.getCurrentState();

        assertThat(state.status()).isEqualTo(GlobalSplitInstallSessionStatus.PENDING);
        assertThat(currentState).isEqualTo(state);
    }

    @Test
    public void whenApkDownloadRequestEmitsRunning_getCurrentState_shouldBeRunning() {
        ApkDownloadRequest.Status runningStatus = new ApkDownloadRequest.Status.Running(25, 20);
        mockDownloadStatusEmission(runningStatus);

        globallyDynamicInstallTask.start();
        GlobalSplitInstallSessionState state = globallyDynamicInstallTask.getCurrentState();

        assertThat(state.status()).isEqualTo(GlobalSplitInstallSessionStatus.DOWNLOADING);
        assertThat(state.bytesDownloaded()).isEqualTo(runningStatus.bytesDownloaded);
        assertThat(state.totalBytesToDownload()).isEqualTo(runningStatus.totalBytes);
        assertThat(currentState).isEqualTo(state);
    }

    @Test
    public void whenApkDownloadRequestEmitsUnknown_getCurrentState_shouldBeUnknown() {
        mockDownloadStatusEmission(new ApkDownloadRequest.Status.Unknown(0, 0, 0));

        globallyDynamicInstallTask.start();
        GlobalSplitInstallSessionState state = globallyDynamicInstallTask.getCurrentState();

        assertThat(state.status()).isEqualTo(GlobalSplitInstallSessionStatus.UNKNOWN);
        assertThat(currentState).isEqualTo(state);
    }

    @Test
    public void whenApkDownloadRequestEmitsPaused_getCurrentState_shouldBePending() {
        ApkDownloadRequest.Status pausedStatus = new ApkDownloadRequest.Status.Paused(25, 20);
        mockDownloadStatusEmission(pausedStatus);

        globallyDynamicInstallTask.start();
        GlobalSplitInstallSessionState state = globallyDynamicInstallTask.getCurrentState();

        assertThat(state.status()).isEqualTo(GlobalSplitInstallSessionStatus.PENDING);
        assertThat(state.bytesDownloaded()).isEqualTo(pausedStatus.bytesDownloaded);
        assertThat(state.totalBytesToDownload()).isEqualTo(pausedStatus.totalBytes);
        assertThat(currentState).isEqualTo(state);
    }

    @Test
    public void whenApkDownloadRequestEmitsFailed_getCurrentState_shouldBeFailed() {
        ApkDownloadRequest.Status failedStatus = new ApkDownloadRequest.Status.Failed(25, 20, 5);
        mockDownloadStatusEmission(failedStatus);

        globallyDynamicInstallTask.start();
        GlobalSplitInstallSessionState state = globallyDynamicInstallTask.getCurrentState();

        assertThat(state.status()).isEqualTo(GlobalSplitInstallSessionStatus.FAILED);
        assertThat(globallyDynamicInstallTask.getException()).isNotNull();
        assertThat(currentState).isEqualTo(state);
    }

    @Test
    public void whenApkDownloadRequestEmitsCanceled_getCurrentState_shouldBeCanceled() {
        ApkDownloadRequest.Status canceledStatus = new ApkDownloadRequest.Status.Canceled(25, 20);
        mockDownloadStatusEmission(canceledStatus);

        globallyDynamicInstallTask.start();
        GlobalSplitInstallSessionState state = globallyDynamicInstallTask.getCurrentState();

        assertThat(state.status()).isEqualTo(GlobalSplitInstallSessionStatus.CANCELED);
        assertThat(state.bytesDownloaded()).isEqualTo(canceledStatus.bytesDownloaded);
        assertThat(state.totalBytesToDownload()).isEqualTo(canceledStatus.totalBytes);
        assertThat(currentState).isEqualTo(state);
    }

    @Test
    public void whenApkDownloadRequestEmitsSuccessful_getCurrentState_shouldBeDownloaded() {
        List<File> apks = Lists.newArrayList(new File(""));
        ApkDownloadRequest.Status successful = new ApkDownloadRequest.Status.Successful(25, 20, apks);
        mockDownloadStatusEmission(successful);
        ApkInstaller mockApkInstaller = mock(ApkInstaller.class);
        doReturn(mockApkInstaller).when(globallyDynamicInstallTask).createApkInstaller(
                any(ApkInstaller.StatusListener.class)
        );

        globallyDynamicInstallTask.start();
        GlobalSplitInstallSessionState state = globallyDynamicInstallTask.getCurrentState();

        assertThat(state.status()).isEqualTo(GlobalSplitInstallSessionStatus.DOWNLOADED);
        assertThat(state.bytesDownloaded()).isEqualTo(successful.bytesDownloaded);
        assertThat(state.totalBytesToDownload()).isEqualTo(successful.totalBytes);
        assertThat(currentState).isEqualTo(state);
        verify(mockApkInstaller).install(apks);
    }

    @Test
    public void whenApkInstallerEmitsInstalling_getCurrentState_shouldBeInstalling() {
        mockApkInstallerStatusEmission(new ApkInstaller.Status.Installing());

        globallyDynamicInstallTask.start();
        GlobalSplitInstallSessionState state = globallyDynamicInstallTask.getCurrentState();

        assertThat(state.status()).isEqualTo(GlobalSplitInstallSessionStatus.INSTALLING);
        assertThat(currentState).isEqualTo(state);
    }

    @Test
    public void whenApkInstallerEmitsPending_getCurrentState_shouldBePending() {
        mockApkInstallerStatusEmission(new ApkInstaller.Status.Pending(""));

        globallyDynamicInstallTask.start();
        GlobalSplitInstallSessionState state = globallyDynamicInstallTask.getCurrentState();

        assertThat(state.status()).isEqualTo(GlobalSplitInstallSessionStatus.PENDING);
        assertThat(currentState).isEqualTo(state);
    }

    @Test
    public void whenApkInstallerEmitsFailed_getCurrentState_shouldBeFailed() {
        IllegalStateException illegalStateException = new IllegalStateException();
        mockApkInstallerStatusEmission(new ApkInstaller.Status.Failed("", 1, illegalStateException));

        globallyDynamicInstallTask.start();
        GlobalSplitInstallSessionState state = globallyDynamicInstallTask.getCurrentState();

        assertThat(state.status()).isEqualTo(GlobalSplitInstallSessionStatus.FAILED);
        assertThat(globallyDynamicInstallTask.getException()).isNotNull();
        assertThat(currentState).isEqualTo(state);
    }

    @Test
    public void whenApkInstallerEmitsInstalled_getCurrentState_shouldBeInstalled() {
        mockApkInstallerStatusEmission(new ApkInstaller.Status.Installed(""));

        globallyDynamicInstallTask.start();
        GlobalSplitInstallSessionState state = globallyDynamicInstallTask.getCurrentState();

        assertThat(state.status()).isEqualTo(GlobalSplitInstallSessionStatus.INSTALLED);
        assertThat(currentState).isEqualTo(state);
    }

    @Test
    public void whenSuccessfulStatusIsEmitted_listenersShouldNotBeNotified() {
        mockApkInstallerStatusEmission(new ApkInstaller.Status.Installed(""));
        final boolean[] calledOnSuccess = new boolean[]{false};
        final boolean[] calledOnComplete = new boolean[]{false};
        globallyDynamicInstallTask.addOnSuccessListener(new OnGlobalSplitInstallSuccessListener<Integer>() {
            @Override
            public void onSuccess(Integer result) {
                calledOnSuccess[0] = true;
            }
        });
        globallyDynamicInstallTask.addOnCompleteListener(new OnGlobalSplitInstallCompleteListener<Integer>() {
            @Override
            public void onComplete(GlobalSplitInstallTask<Integer> task) {
                calledOnComplete[0] = true;
            }
        });

        globallyDynamicInstallTask.start();

        assertThat(calledOnSuccess[0]).isFalse();
        assertThat(calledOnComplete[0]).isFalse();
    }

    @Test
    public void whenFailedStatusIsEmitted_listenersShouldBeNotified() {
        mockApkInstallerStatusEmission(new ApkInstaller.Status.Failed("", 1, new IllegalStateException()));
        final boolean[] calledOnFailed = new boolean[]{false};
        final boolean[] calledOnComplete = new boolean[]{false};
        globallyDynamicInstallTask.addOnFailureListener(new OnGlobalSplitInstallFailureListener() {
            @Override
            public void onFailure(Exception e) {
                calledOnFailed[0] = true;
            }
        });
        globallyDynamicInstallTask.addOnCompleteListener(new OnGlobalSplitInstallCompleteListener<Integer>() {
            @Override
            public void onComplete(GlobalSplitInstallTask<Integer> task) {
                calledOnComplete[0] = true;
            }
        });

        globallyDynamicInstallTask.start();

        assertThat(calledOnFailed[0]).isTrue();
        assertThat(calledOnComplete[0]).isTrue();
    }

    @Test
    public void whenDownloading_cancel_downloadShouldBeCanceled() {
        ApkDownloadRequest mockApkDownloadRequest = mockDownloadStatusEmission(
                new ApkDownloadRequest.Status.Running(1, 1));
        globallyDynamicInstallTask.start();

        globallyDynamicInstallTask.cancel(1);

        verify(mockApkDownloadRequest).cancel();
    }

    @Test(expected = IllegalStateException.class)
    public void whenDownloadHasNotStarted_getResult_shouldThrowIllegalStateException() {
        globallyDynamicInstallTask.getResult();
    }

    @Test
    public void whenDownloadHasStarted_getResult_shouldReturnSessionId() {
        int downloadId = 50;
        mockDownloadStatusEmission(new ApkDownloadRequest.Status.Enqueued(downloadId));

        globallyDynamicInstallTask.start();
        int result = globallyDynamicInstallTask.getResult();

        assertThat(result).isEqualTo(downloadId);
    }

    @Test(expected = RuntimeExecutionException.class)
    public void whenTaskHasFailed_getResult_shouldThrowRuntimeExecutionException() {
        final IllegalStateException illegalStateException = new IllegalStateException("yo");
        when(mockGloballyDynamicConfigurationRepository.getConfiguration()).thenReturn(Result.of(illegalStateException));

        globallyDynamicInstallTask.start();
        globallyDynamicInstallTask.getResult();
    }

    @Test
    public void whenFailedStatusIsEmitted_shouldUnregisterSelf() {
        mockDownloadStatusEmission(new ApkDownloadRequest.Status.Failed(0, 0, 1));

        globallyDynamicInstallTask.start();

        verify(mockTaskRegistry).unregisterTask(globallyDynamicInstallTask);
    }

    @Test
    public void whenSuccessStatusIsEmitted_shouldUnregisterSelf() {
        mockApkInstallerStatusEmission(new ApkInstaller.Status.Installed("hi"));

        globallyDynamicInstallTask.start();

        verify(mockTaskRegistry).unregisterTask(globallyDynamicInstallTask);
    }

    @Test
    public void whenCancelStatusIsEmitted_shouldUnregisterSelf() {
        mockDownloadStatusEmission(new ApkDownloadRequest.Status.Canceled(0, 0));

        globallyDynamicInstallTask.start();

        verify(mockTaskRegistry).unregisterTask(globallyDynamicInstallTask);
    }
}