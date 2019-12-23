package com.jeppeman.locallydynamic;

import android.app.Activity;
import android.content.IntentSender;
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.play.core.splitinstall.SplitInstallException;
import com.google.android.play.core.splitinstall.SplitInstallManager;
import com.google.android.play.core.splitinstall.SplitInstallRequest;
import com.google.android.play.core.splitinstall.SplitInstallSessionState;
import com.google.android.play.core.splitinstall.model.SplitInstallErrorCode;
import com.google.android.play.core.tasks.Task;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
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
import org.robolectric.annotation.Config;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class LocallyDynamicSplitInstallManagerImplTest {
    private LocallyDynamicSplitInstallManagerImpl locallyDynamicSplitInstallManager;
    @Mock
    private Executor mockExecutor;
    @Mock
    private SplitInstallManager mockSplitInstallManager;
    @Mock
    private GLExtensionsExtractor mockGlExtensionsExtractor;
    @Mock
    private LocallyDynamicApi mockLocallyDynamicApi;
    @Mock
    private LocallyDynamicConfigurationRepository mockLocallyDynamicConfigurationRepository;
    @Mock
    private TaskRegistry mockTaskRegistry;
    @Mock
    private Logger mockLogger;
    @Captor
    private ArgumentCaptor<Executor.Callbacks<?>> callbacksArgumentCaptor;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        locallyDynamicSplitInstallManager = new LocallyDynamicSplitInstallManagerImpl(
                ApplicationProvider.getApplicationContext(),
                new LocallyDynamicBuildConfig(),
                mockExecutor,
                mockSplitInstallManager,
                mockGlExtensionsExtractor,
                mockLocallyDynamicApi,
                mockLocallyDynamicConfigurationRepository,
                mockTaskRegistry,
                mockLogger
        );

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                Executor.Callbacks callbacks = invocation.getArgument(0);
                callbacks.onComplete(callbacks.execute());
                return null;
            }
        }).when(mockExecutor).executeForeground(callbacksArgumentCaptor.capture());
    }

    @Test
    public void getInstalledModules_shouldDelegateToActualSplitInstallManager() {
        Set<String> modules = Sets.newHashSet("a", "b", "c");
        when(mockSplitInstallManager.getInstalledModules()).thenReturn(modules);

        Set<String> installedModules = locallyDynamicSplitInstallManager.getInstalledModules();

        verify(mockSplitInstallManager).getInstalledModules();
        assertThat(installedModules).isEqualTo(modules);
    }

    @Test
    public void getInstalledLanguages_shouldDelegateToActualSplitInstallManager() {
        Set<String> languages = Sets.newHashSet("a", "b", "c");
        when(mockSplitInstallManager.getInstalledModules()).thenReturn(languages);

        Set<String> installedLanguages = locallyDynamicSplitInstallManager.getInstalledModules();

        verify(mockSplitInstallManager).getInstalledModules();
        assertThat(installedLanguages).isEqualTo(languages);
    }

    @Test
    public void deferredLanguageUninstall_shouldDelegateToActualSplitInstallManager() {
        List<Locale> locales = Lists.newArrayList(Locale.ENGLISH);

        locallyDynamicSplitInstallManager.deferredLanguageUninstall(locales);

        verify(mockSplitInstallManager).deferredLanguageUninstall(locales);
    }

    @Test
    public void deferredLanguageInstall_shouldDelegateToActualSplitInstallManager() {
        List<Locale> locales = Lists.newArrayList(Locale.ENGLISH);

        locallyDynamicSplitInstallManager.deferredLanguageInstall(locales);

        verify(mockSplitInstallManager).deferredLanguageInstall(locales);
    }

    @Test
    public void deferredInstall_shouldDelegateToActualSplitInstallManager() {
        List<String> modules = Lists.newArrayList("a", "b");

        locallyDynamicSplitInstallManager.deferredInstall(modules);

        verify(mockSplitInstallManager).deferredInstall(modules);
    }

    @Test
    public void deferredUninstall_shouldDelegateToActualSplitInstallManager() {
        List<String> modules = Lists.newArrayList("a", "b");

        locallyDynamicSplitInstallManager.deferredUninstall(modules);

        verify(mockSplitInstallManager).deferredUninstall(modules);
    }

    @Test
    public void startConfirmationDialogForResult_shouldDelegateToActualSplitInstallManager() throws IntentSender.SendIntentException {
        SplitInstallSessionState state = mock(SplitInstallSessionState.class);
        Activity activity = mock(Activity.class);
        int requestCode = 10;

        locallyDynamicSplitInstallManager.startConfirmationDialogForResult(
                state, activity, requestCode
        );

        verify(mockSplitInstallManager).startConfirmationDialogForResult(state, activity, requestCode);
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.KITKAT)
    public void whenSdkIsOlderThan21_getSessionState_shouldFailWithApiUnavailable() {
        Task<SplitInstallSessionState> task = locallyDynamicSplitInstallManager.getSessionState(0);

        assertThat(((SplitInstallException) task.getException()).getErrorCode()).isEqualTo(SplitInstallErrorCode.API_NOT_AVAILABLE);
    }

    @Test
    public void whenTaskIsNotPresent_getSessionState_shouldFailWithInvalidRequest() {
        Task<SplitInstallSessionState> task = locallyDynamicSplitInstallManager.getSessionState(0);

        assertThat(((SplitInstallException) task.getException()).getErrorCode()).isEqualTo(SplitInstallErrorCode.INVALID_REQUEST);
    }

    @Test
    public void whenTaskIsPresent_getSessionState_shouldReturnState() {
        int sessionId = 1;
        LocallyDynamicInstallTask mockTask = mock(LocallyDynamicInstallTask.class);
        SplitInstallSessionState mockState = mock(SplitInstallSessionState.class);
        when(mockTask.getCurrentState()).thenReturn(mockState);
        when(mockTaskRegistry.findTaskBySessionId(sessionId)).thenReturn(mockTask);

        Task<SplitInstallSessionState> task = locallyDynamicSplitInstallManager.getSessionState(sessionId);

        assertThat(task.getResult()).isEqualTo(mockState);
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.KITKAT)
    public void whenSdkIsOlderThan21_getSessionStates_shouldFailWithApiUnavailable() {
        Task<List<SplitInstallSessionState>> task = locallyDynamicSplitInstallManager.getSessionStates();

        assertThat(((SplitInstallException) task.getException()).getErrorCode()).isEqualTo(SplitInstallErrorCode.API_NOT_AVAILABLE);
    }

    @Test
    public void whenTasksArePresent_getSessionState_shouldReturnStates() {
        LocallyDynamicInstallTask mockTask = mock(LocallyDynamicInstallTask.class);
        SplitInstallSessionState mockState = mock(SplitInstallSessionState.class);
        when(mockTask.getCurrentState()).thenReturn(mockState);
        LocallyDynamicInstallTask mockTask2 = mock(LocallyDynamicInstallTask.class);
        SplitInstallSessionState mockState2 = mock(SplitInstallSessionState.class);
        when(mockTask2.getCurrentState()).thenReturn(mockState2);
        when(mockTaskRegistry.getTasks()).thenReturn(Lists.newArrayList(mockTask, mockTask2));

        Task<List<SplitInstallSessionState>> task = locallyDynamicSplitInstallManager.getSessionStates();

        assertThat(task.getResult()).isEqualTo(Lists.newArrayList(mockState, mockState2));
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.KITKAT)
    public void whenSdkIsOlderThan21_cancelInstall_shouldFailWithApiUnavailable() {
        Task<Void> task = locallyDynamicSplitInstallManager.cancelInstall(0);

        assertThat(((SplitInstallException) task.getException()).getErrorCode()).isEqualTo(SplitInstallErrorCode.API_NOT_AVAILABLE);
    }

    @Test
    public void whenTaskIsNotPresent_cancelInstall_shouldFailWithInvalidRequest() {
        Task<Void> task = locallyDynamicSplitInstallManager.cancelInstall(0);

        assertThat(((SplitInstallException) task.getException()).getErrorCode()).isEqualTo(SplitInstallErrorCode.INVALID_REQUEST);
    }

    @Test
    public void whenTaskIsPresent_cancelInstall_shouldCallCancelOnIt() {
        int sessionId = 1;
        LocallyDynamicInstallTask mockTask = mock(LocallyDynamicInstallTask.class);
        SplitInstallSessionState mockState = mock(SplitInstallSessionState.class);
        when(mockTask.getCurrentState()).thenReturn(mockState);
        when(mockTaskRegistry.findTaskBySessionId(sessionId)).thenReturn(mockTask);

        Task<Void> task = locallyDynamicSplitInstallManager.cancelInstall(sessionId);

        verify(mockTask).cancel(sessionId);
        assertThat(task.getResult()).isNull();
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.KITKAT)
    public void whenSdkIsOlderThan21_startInstall_shouldFailWithApiUnavailable() {
        Task<Integer> task = locallyDynamicSplitInstallManager.startInstall(SplitInstallRequest.newBuilder().build());

        assertThat(((SplitInstallException) task.getException()).getErrorCode()).isEqualTo(SplitInstallErrorCode.API_NOT_AVAILABLE);
    }

    @Test
    public void whenAllModulesAndLanguagesAreInstalled_startInstall_shouldReturnImmediately() {
        when(mockSplitInstallManager.getInstalledModules()).thenReturn(Sets.newHashSet("a", "b"));
        when(mockSplitInstallManager.getInstalledLanguages()).thenReturn(Sets.newHashSet("c", "d"));

        Task<Integer> task = locallyDynamicSplitInstallManager.startInstall(SplitInstallRequest.newBuilder()
                .addModule("a")
                .addModule("b")
                .addLanguage(new Locale("c", "c"))
                .addLanguage(new Locale("d", "d"))
                .build()
        );

        assertThat(task.getResult()).isEqualTo(0);
    }

    @Test
    public void whenAllModulesAndLanguagesAreNotInstalled_startInstall_shouldRegisterTaskAndAddCompletionListener() {
        when(mockSplitInstallManager.getInstalledModules()).thenReturn(Sets.newHashSet("a"));
        when(mockSplitInstallManager.getInstalledLanguages()).thenReturn(Sets.newHashSet("c"));
        locallyDynamicSplitInstallManager.taskCounter.set(8);

        LocallyDynamicInstallTaskImpl task = (LocallyDynamicInstallTaskImpl) locallyDynamicSplitInstallManager.startInstall(SplitInstallRequest.newBuilder()
                .addModule("a")
                .addModule("b")
                .addLanguage(new Locale("c", "c"))
                .addLanguage(new Locale("d", "d"))
                .build()
        );

        verify(mockTaskRegistry).registerTask(task);
        verify(mockTaskRegistry, never()).unregisterTask(task);
        task.notifyCompleted();
        verify(mockTaskRegistry).unregisterTask(task);
        assertThat(task.getCurrentState().sessionId()).isEqualTo(locallyDynamicSplitInstallManager.taskCounter.get());
    }
}