package com.jeppeman.locallydynamic;

import android.content.Context;

import com.google.android.play.core.splitinstall.SplitInstallRequest;
import com.google.android.play.core.splitinstall.SplitInstallSessionState;
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener;
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus;
import com.google.android.play.core.tasks.OnCompleteListener;
import com.google.android.play.core.tasks.OnFailureListener;
import com.google.android.play.core.tasks.OnSuccessListener;
import com.google.android.play.core.tasks.RuntimeExecutionException;
import com.google.android.play.core.tasks.Task;
import com.google.common.collect.Lists;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LocallyDynamicInstallTaskImplTest {
    private LocallyDynamicInstallTaskImpl locallyDynamicInstallTask;
    @Mock
    private LocallyDynamicConfigurationRepository mockLocallyDynamicConfigurationRepository;
    @Mock
    private Context mockContext;
    @Mock
    private Executor mockExecutor;
    @Mock
    private Logger mockLogger;
    private SplitInstallSessionState currentState;
    private int id = 5;

    private SplitInstallRequest splitInstallRequest = SplitInstallRequest.newBuilder()
            .addLanguage(Locale.ENGLISH)
            .addLanguage(Locale.CANADA)
            .addModule("a")
            .addModule("b")
            .build();

    private LocallyDynamicConfigurationDto locallyDynamicConfigurationDto =
            new LocallyDynamicConfigurationDto(
                    "deviceId",
                    "serverUrl",
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

    @Before
    public void setUp() {
        locallyDynamicInstallTask = spy(new LocallyDynamicInstallTaskImpl(
                mockLocallyDynamicConfigurationRepository,
                mockContext,
                Lists.<SplitInstallStateUpdatedListener>newArrayList(
                        new SplitInstallStateUpdatedListener() {
                            @Override
                            public void onStateUpdate(SplitInstallSessionState state) {
                                currentState = state;
                            }
                        }),
                splitInstallRequest,
                mockExecutor,
                mockLogger,
                id
        ));

        setupExecutorMock();
    }

    @After
    public void tearDown() {
        Mockito.reset();
    }

    private void setupExecutorMock() {
        doAnswer(new Answer() {
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

        doAnswer(new Answer() {
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
        final ApkDownloadRequest mockDownloadRequest = mock(ApkDownloadRequest.class);
        when(mockLocallyDynamicConfigurationRepository.getConfiguration()).thenReturn(Result.of(locallyDynamicConfigurationDto));
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                ApkDownloadRequest.StatusListener statusListener = invocation.getArgument(1);
                statusListener.onUpdate(status);
                return mockDownloadRequest;
            }
        }).when(locallyDynamicInstallTask).createApkDownloadRequest(
                eq(locallyDynamicConfigurationDto),
                ArgumentCaptor.<ApkDownloadRequest.StatusListener, ApkDownloadRequest.StatusListener>
                        forClass(ApkDownloadRequest.StatusListener.class).capture()
        );

        return mockDownloadRequest;
    }

    private void mockApkInstallerStatusEmission(final ApkInstaller.Status status) {
        mockDownloadStatusEmission(new ApkDownloadRequest.Status.Successful(0, 0, new ArrayList<File>()));
        final ApkInstaller mockApkInstaller = mock(ApkInstaller.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                ApkInstaller.StatusListener statusListener = invocation.getArgument(0);
                statusListener.onUpdate(status);
                return mockApkInstaller;
            }
        }).when(locallyDynamicInstallTask).createApkInstaller(
                ArgumentCaptor.<ApkInstaller.StatusListener, ApkInstaller.StatusListener>
                        forClass(ApkInstaller.StatusListener.class).capture()
        );
    }

    @Test
    public void whenConfigurationIsSuccessfullyFetched_start_shouldStartDownload() {
        ApkDownloadRequest mockDownloadRequest = mock(ApkDownloadRequest.class);
        when(mockLocallyDynamicConfigurationRepository.getConfiguration()).thenReturn(Result.of(locallyDynamicConfigurationDto));
        doReturn(mockDownloadRequest).when(locallyDynamicInstallTask).createApkDownloadRequest(
                eq(locallyDynamicConfigurationDto),
                ArgumentCaptor.<ApkDownloadRequest.StatusListener, ApkDownloadRequest.StatusListener>
                        forClass(ApkDownloadRequest.StatusListener.class).capture()
        );

        locallyDynamicInstallTask.start();

        verify(mockDownloadRequest).start();
    }

    @Test
    public void whenConfigurationFetchFails_start_shouldFailTask() {
        final IllegalStateException illegalStateException = new IllegalStateException("yo");
        when(mockLocallyDynamicConfigurationRepository.getConfiguration()).thenReturn(Result.of(illegalStateException));

        locallyDynamicInstallTask.start();

        assertThat(locallyDynamicInstallTask.getCurrentState().status()).isEqualTo(SplitInstallSessionStatus.FAILED);
        assertThat(locallyDynamicInstallTask.getException()).isEqualTo(illegalStateException);
        assertThat(locallyDynamicInstallTask.isSuccessful()).isFalse();
        assertThat(locallyDynamicInstallTask.getCurrentState()).isEqualTo(currentState);
    }

    @Test
    public void whenConfigurationFetchThrows_start_shouldFailTask() {
        final IllegalStateException illegalStateException = new IllegalStateException("yo");
        final boolean[] calledOnFailure = new boolean[]{false};
        when(mockLocallyDynamicConfigurationRepository.getConfiguration()).thenThrow(illegalStateException);
        locallyDynamicInstallTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                calledOnFailure[0] = true;
            }
        });

        locallyDynamicInstallTask.start();

        assertThat(calledOnFailure[0]).isTrue();
        assertThat(locallyDynamicInstallTask.getException()).isEqualTo(illegalStateException);
        assertThat(locallyDynamicInstallTask.isSuccessful()).isFalse();
        assertThat(currentState.status()).isEqualTo(SplitInstallSessionStatus.FAILED);
        assertThat(locallyDynamicInstallTask.getCurrentState().status()).isEqualTo(SplitInstallSessionStatus.FAILED);
    }

    @Test
    public void whenApkDownloadRequestEmitsPending_getCurrentState_shouldBePending() {
        mockDownloadStatusEmission(new ApkDownloadRequest.Status.Pending(0, 0));

        locallyDynamicInstallTask.start();
        SplitInstallSessionState state = locallyDynamicInstallTask.getCurrentState();

        assertThat(state.status()).isEqualTo(SplitInstallSessionStatus.PENDING);
        assertThat(currentState).isEqualTo(state);
    }

    @Test
    public void whenApkDownloadRequestEmitsRunning_getCurrentState_shouldBeRunning() {
        ApkDownloadRequest.Status runningStatus = new ApkDownloadRequest.Status.Running(25, 20);
        mockDownloadStatusEmission(runningStatus);

        locallyDynamicInstallTask.start();
        SplitInstallSessionState state = locallyDynamicInstallTask.getCurrentState();

        assertThat(state.status()).isEqualTo(SplitInstallSessionStatus.DOWNLOADING);
        assertThat(state.bytesDownloaded()).isEqualTo(runningStatus.bytesDownloaded);
        assertThat(state.totalBytesToDownload()).isEqualTo(runningStatus.totalBytes);
        assertThat(currentState).isEqualTo(state);
    }

    @Test
    public void whenApkDownloadRequestEmitsUnknown_getCurrentState_shouldBeUnknown() {
        mockDownloadStatusEmission(new ApkDownloadRequest.Status.Unknown(0, 0, 0));

        locallyDynamicInstallTask.start();
        SplitInstallSessionState state = locallyDynamicInstallTask.getCurrentState();

        assertThat(state.status()).isEqualTo(SplitInstallSessionStatus.UNKNOWN);
        assertThat(currentState).isEqualTo(state);
    }

    @Test
    public void whenApkDownloadRequestEmitsPaused_getCurrentState_shouldBePending() {
        ApkDownloadRequest.Status pausedStatus = new ApkDownloadRequest.Status.Paused(25, 20);
        mockDownloadStatusEmission(pausedStatus);

        locallyDynamicInstallTask.start();
        SplitInstallSessionState state = locallyDynamicInstallTask.getCurrentState();

        assertThat(state.status()).isEqualTo(SplitInstallSessionStatus.PENDING);
        assertThat(state.bytesDownloaded()).isEqualTo(pausedStatus.bytesDownloaded);
        assertThat(state.totalBytesToDownload()).isEqualTo(pausedStatus.totalBytes);
        assertThat(currentState).isEqualTo(state);
    }

    @Test
    public void whenApkDownloadRequestEmitsFailed_getCurrentState_shouldBeFailed() {
        ApkDownloadRequest.Status failedStatus = new ApkDownloadRequest.Status.Failed(25, 20, 5);
        mockDownloadStatusEmission(failedStatus);

        locallyDynamicInstallTask.start();
        SplitInstallSessionState state = locallyDynamicInstallTask.getCurrentState();

        assertThat(state.status()).isEqualTo(SplitInstallSessionStatus.FAILED);
        assertThat(locallyDynamicInstallTask.getException()).isNotNull();
        assertThat(currentState).isEqualTo(state);
    }

    @Test
    public void whenApkDownloadRequestEmitsCanceled_getCurrentState_shouldBeCanceled() {
        ApkDownloadRequest.Status canceledStatus = new ApkDownloadRequest.Status.Canceled(25, 20);
        mockDownloadStatusEmission(canceledStatus);

        locallyDynamicInstallTask.start();
        SplitInstallSessionState state = locallyDynamicInstallTask.getCurrentState();

        assertThat(state.status()).isEqualTo(SplitInstallSessionStatus.CANCELED);
        assertThat(locallyDynamicInstallTask.isComplete()).isTrue();
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
        doReturn(mockApkInstaller).when(locallyDynamicInstallTask).createApkInstaller(
                any(ApkInstaller.StatusListener.class)
        );

        locallyDynamicInstallTask.start();
        SplitInstallSessionState state = locallyDynamicInstallTask.getCurrentState();

        assertThat(state.status()).isEqualTo(SplitInstallSessionStatus.DOWNLOADED);
        assertThat(state.bytesDownloaded()).isEqualTo(successful.bytesDownloaded);
        assertThat(state.totalBytesToDownload()).isEqualTo(successful.totalBytes);
        assertThat(currentState).isEqualTo(state);
        verify(mockApkInstaller).install(apks);
    }

    @Test
    public void whenApkInstallerEmitsInstalling_getCurrentState_shouldBeInstalling() {
        mockApkInstallerStatusEmission(new ApkInstaller.Status.Installing());

        locallyDynamicInstallTask.start();
        SplitInstallSessionState state = locallyDynamicInstallTask.getCurrentState();

        assertThat(state.status()).isEqualTo(SplitInstallSessionStatus.INSTALLING);
        assertThat(currentState).isEqualTo(state);
    }

    @Test
    public void whenApkInstallerEmitsPending_getCurrentState_shouldBePending() {
        mockApkInstallerStatusEmission(new ApkInstaller.Status.Pending(""));

        locallyDynamicInstallTask.start();
        SplitInstallSessionState state = locallyDynamicInstallTask.getCurrentState();

        assertThat(state.status()).isEqualTo(SplitInstallSessionStatus.PENDING);
        assertThat(currentState).isEqualTo(state);
    }

    @Test
    public void whenApkInstallerEmitsFailed_getCurrentState_shouldBeFailed() {
        IllegalStateException illegalStateException = new IllegalStateException();
        mockApkInstallerStatusEmission(new ApkInstaller.Status.Failed("", 1, illegalStateException));

        locallyDynamicInstallTask.start();
        SplitInstallSessionState state = locallyDynamicInstallTask.getCurrentState();

        assertThat(state.status()).isEqualTo(SplitInstallSessionStatus.FAILED);
        assertThat(locallyDynamicInstallTask.getException()).isEqualTo(illegalStateException);
        assertThat(currentState).isEqualTo(state);
    }

    @Test
    public void whenApkInstallerEmitsInstalled_getCurrentState_shouldBeInstalled() {
        mockApkInstallerStatusEmission(new ApkInstaller.Status.Installed(""));

        locallyDynamicInstallTask.start();
        SplitInstallSessionState state = locallyDynamicInstallTask.getCurrentState();

        assertThat(state.status()).isEqualTo(SplitInstallSessionStatus.INSTALLED);
        assertThat(locallyDynamicInstallTask.getResult()).isEqualTo(SplitInstallSessionStatus.INSTALLED);
        assertThat(currentState).isEqualTo(state);
    }

    @Test
    public void whenSuccessful_listenersShouldBeNotified() {
        mockApkInstallerStatusEmission(new ApkInstaller.Status.Installed(""));
        final boolean[] calledOnSuccess = new boolean[]{false};
        final boolean[] calledOnComplete = new boolean[]{false};
        locallyDynamicInstallTask.addOnSuccessListener(new OnSuccessListener<Integer>() {
            @Override
            public void onSuccess(Integer result) {
                calledOnSuccess[0] = true;
            }
        });
        locallyDynamicInstallTask.addOnCompleteListener(new OnCompleteListener<Integer>() {
            @Override
            public void onComplete(Task<Integer> task) {
                calledOnComplete[0] = true;
            }
        });

        locallyDynamicInstallTask.start();

        assertThat(calledOnSuccess[0]).isTrue();
        assertThat(calledOnComplete[0]).isTrue();
    }

    @Test
    public void whenFailed_listenersShouldBeNotified() {
        mockApkInstallerStatusEmission(new ApkInstaller.Status.Failed("", 1, new IllegalStateException()));
        final boolean[] calledOnFailed = new boolean[]{false};
        final boolean[] calledOnComplete = new boolean[]{false};
        locallyDynamicInstallTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                calledOnFailed[0] = true;
            }
        });
        locallyDynamicInstallTask.addOnCompleteListener(new OnCompleteListener<Integer>() {
            @Override
            public void onComplete(Task<Integer> task) {
                calledOnComplete[0] = true;
            }
        });

        locallyDynamicInstallTask.start();

        assertThat(calledOnFailed[0]).isTrue();
        assertThat(calledOnComplete[0]).isTrue();
    }

    @Test
    public void whenCanceled_listenersShouldBeNotified() {
        ApkDownloadRequest mockApkDownloadRequest = mockDownloadStatusEmission(
                new ApkDownloadRequest.Status.Running(1, 1));
        final boolean[] calledOnSuccess = new boolean[]{false};
        final boolean[] calledOnComplete = new boolean[]{false};
        locallyDynamicInstallTask.addOnSuccessListener(new OnSuccessListener<Integer>() {
            @Override
            public void onSuccess(Integer result) {
                calledOnSuccess[0] = true;
            }
        });
        locallyDynamicInstallTask.addOnCompleteListener(new OnCompleteListener<Integer>() {
            @Override
            public void onComplete(Task<Integer> task) {
                calledOnComplete[0] = true;
            }
        });
        locallyDynamicInstallTask.start();

        locallyDynamicInstallTask.cancel(1);

        assertThat(calledOnSuccess[0]).isFalse();
        assertThat(calledOnComplete[0]).isTrue();
        verify(mockApkDownloadRequest).cancel();
    }

    @Test(expected = IllegalStateException.class)
    public void whenNotComplete_getResult_shouldThrowIllegalState() {
        locallyDynamicInstallTask.getResult();
    }

    @Test(expected = RuntimeExecutionException.class)
    public void whenFailed_getResult_shouldThrowRuntimeExecution() {
        mockDownloadStatusEmission(new ApkDownloadRequest.Status.Failed(0,0,0));
        locallyDynamicInstallTask.start();

        locallyDynamicInstallTask.getResult();
    }
}