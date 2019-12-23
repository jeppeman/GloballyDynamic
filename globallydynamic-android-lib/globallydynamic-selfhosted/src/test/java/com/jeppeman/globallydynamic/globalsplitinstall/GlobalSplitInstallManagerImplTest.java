package com.jeppeman.globallydynamic.globalsplitinstall;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Build;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.jeppeman.globallydynamic.generated.GloballyDynamicBuildConfig;
import com.jeppeman.globallydynamic.generated.ModuleConditions;
import com.jeppeman.globallydynamic.net.HttpClient;
import com.jeppeman.globallydynamic.tasks.GlobalSplitInstallTask;

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

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class GlobalSplitInstallManagerImplTest {
    private GlobalSplitInstallManagerImpl globallyDynamicSplitInstallManager;
    @Mock
    private Executor mockExecutor;
    @Mock
    private GlobalSplitInstallManager mockGlobalSplitInstallManager;
    @Mock
    private GloballyDynamicConfigurationRepository mockGloballyDynamicConfigurationRepository;
    @Mock
    private GloballyDynamicBuildConfig mockGloballyDynamicBuildConfig;
    @Mock
    private TaskRegistry mockTaskRegistry;
    @Mock
    private Logger mockLogger;
    @Mock
    private SignatureProvider mockSignatureProvider;
    @Mock
    private ApplicationPatcher mockApplicationPatcher;
    @Mock
    private MissingSplitsManager mockMissingSplitsManager;
    @Mock
    private HttpClient mockHttpClient;
    @Captor
    private ArgumentCaptor<Executor.Callbacks<?>> callbacksArgumentCaptor;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        globallyDynamicSplitInstallManager = new GlobalSplitInstallManagerImpl(
                ApplicationProvider.getApplicationContext(),
                mockExecutor,
                mockGlobalSplitInstallManager,
                mockGloballyDynamicConfigurationRepository,
                mockGloballyDynamicBuildConfig,
                mockTaskRegistry,
                mockLogger,
                mockApplicationPatcher,
                mockMissingSplitsManager,
                mockSignatureProvider,
                mockHttpClient
        );

        when(mockGloballyDynamicBuildConfig.getInstallTimeFeatures()).thenReturn(Maps.<String, ModuleConditions>newHashMap());
        when(mockGloballyDynamicBuildConfig.getOnDemandFeatures()).thenReturn(new String[0]);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Exception {
                Executor.Callbacks callbacks = invocation.getArgument(0);
                callbacks.onComplete(callbacks.execute());
                return null;
            }
        }).when(mockExecutor).executeForeground(callbacksArgumentCaptor.capture());
    }

    @Test
    public void getInstalledModules_shouldDelegateToActualSplitInstallManager() {
        Set<String> modules = Sets.newHashSet("a", "b", "c");
        when(mockGlobalSplitInstallManager.getInstalledModules()).thenReturn(modules);

        Set<String> installedModules = globallyDynamicSplitInstallManager.getInstalledModules();

        verify(mockGlobalSplitInstallManager).getInstalledModules();
        assertThat(installedModules).isEqualTo(modules);
    }

    @Test
    public void getInstalledLanguages_shouldDelegateToActualSplitInstallManager() {
        Set<String> languages = Sets.newHashSet("a", "b", "c");
        when(mockGlobalSplitInstallManager.getInstalledModules()).thenReturn(languages);

        Set<String> installedLanguages = globallyDynamicSplitInstallManager.getInstalledModules();

        verify(mockGlobalSplitInstallManager).getInstalledModules();
        assertThat(installedLanguages).isEqualTo(languages);
    }

    @Test
    public void deferredLanguageUninstall_shouldDelegateToActualSplitInstallManager() {
        List<Locale> locales = Lists.newArrayList(Locale.ENGLISH);

        globallyDynamicSplitInstallManager.deferredLanguageUninstall(locales);

        verify(mockGlobalSplitInstallManager).deferredLanguageUninstall(locales);
    }

    @Test
    public void deferredLanguageInstall_shouldDelegateToActualSplitInstallManager() {
        List<Locale> locales = Lists.newArrayList(Locale.ENGLISH);

        globallyDynamicSplitInstallManager.deferredLanguageInstall(locales);

        verify(mockGlobalSplitInstallManager).deferredLanguageInstall(locales);
    }

    @Test
    public void deferredInstall_shouldDelegateToActualSplitInstallManager() {
        List<String> modules = Lists.newArrayList("a", "b");

        globallyDynamicSplitInstallManager.deferredInstall(modules);

        verify(mockGlobalSplitInstallManager).deferredInstall(modules);
    }

    @Test
    public void deferredUninstall_shouldDelegateToActualSplitInstallManager() {
        List<String> modules = Lists.newArrayList("a", "b");

        globallyDynamicSplitInstallManager.deferredUninstall(modules);

        verify(mockGlobalSplitInstallManager).deferredUninstall(modules);
    }

    @Test
    public void whenTaskDoesNotExist_startConfirmationDialogForResult_shouldNotStartUserConfirmationActivity() throws IntentSender.SendIntentException {
        GlobalSplitInstallSessionState state = mock(GlobalSplitInstallSessionState.class);
        Activity activity = mock(Activity.class);
        int requestCode = 10;

        boolean didLaunch = globallyDynamicSplitInstallManager.startConfirmationDialogForResult(
                state, activity, requestCode
        );

        verify(activity, never()).startActivityForResult(any(Intent.class), anyInt());
        assertThat(didLaunch).isFalse();
    }

    @Test
    public void whenTaskDoesExist_startConfirmationDialogForResult_shouldStartUserConfirmationActivity() throws IntentSender.SendIntentException {
        GlobalSplitInstallSessionState state = mock(GlobalSplitInstallSessionState.class);
        Activity activity = mock(Activity.class);
        int sessionId = 305;
        when(state.sessionId()).thenReturn(sessionId);
        int requestCode = 10;
        InstallTask task = mock(InstallTask.class);
        when(task.getCurrentState()).thenReturn(state);
        when(mockTaskRegistry.findTaskBySessionId(sessionId)).thenReturn(task);

        boolean didLaunch = globallyDynamicSplitInstallManager.startConfirmationDialogForResult(
                state, activity, requestCode
        );

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(activity).startActivityForResult(captor.capture(), anyInt());
        assertThat(captor.getValue().getComponent().getClassName()).isEqualTo(UserConfirmationActivity.class.getName());
        assertThat(didLaunch).isTrue();
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.KITKAT)
    public void whenSdkIsOlderThan21_getSessionState_shouldFailWithApiUnavailable() {
        GlobalSplitInstallTask<GlobalSplitInstallSessionState> task = globallyDynamicSplitInstallManager.getSessionState(0);

        assertThat(((GlobalSplitInstallException) task.getException()).getErrorCode()).isEqualTo(GlobalSplitInstallErrorCode.API_NOT_AVAILABLE);
    }

    @Test
    public void whenTaskIsNotPresent_getSessionState_shouldFailWithSessionNotFound() {
        GlobalSplitInstallTask<GlobalSplitInstallSessionState> task = globallyDynamicSplitInstallManager.getSessionState(0);

        assertThat(((GlobalSplitInstallException) task.getException()).getErrorCode()).isEqualTo(GlobalSplitInstallErrorCode.SESSION_NOT_FOUND);
    }

    @Test
    public void whenTaskIsPresent_getSessionState_shouldReturnState() {
        int sessionId = 1;
        InstallTask mockTask = mock(InstallTask.class);
        GlobalSplitInstallSessionState mockState = mock(GlobalSplitInstallSessionState.class);
        when(mockTask.getCurrentState()).thenReturn(mockState);
        when(mockTaskRegistry.findTaskBySessionId(sessionId)).thenReturn(mockTask);

        GlobalSplitInstallTask<GlobalSplitInstallSessionState> task = globallyDynamicSplitInstallManager.getSessionState(sessionId);

        assertThat(task.getResult()).isEqualTo(mockState);
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.KITKAT)
    public void whenSdkIsOlderThan21_getSessionStates_shouldFailWithApiUnavailable() {
        GlobalSplitInstallTask<List<GlobalSplitInstallSessionState>> task = globallyDynamicSplitInstallManager.getSessionStates();

        assertThat(((GlobalSplitInstallException) task.getException()).getErrorCode()).isEqualTo(GlobalSplitInstallErrorCode.API_NOT_AVAILABLE);
    }

    @Test
    public void whenTasksArePresent_getSessionState_shouldReturnStates() {
        InstallTask mockTask = mock(InstallTask.class);
        GlobalSplitInstallSessionState mockState = mock(GlobalSplitInstallSessionState.class);
        when(mockTask.getCurrentState()).thenReturn(mockState);
        InstallTask mockTask2 = mock(InstallTask.class);
        GlobalSplitInstallSessionState mockState2 = mock(GlobalSplitInstallSessionState.class);
        when(mockTask2.getCurrentState()).thenReturn(mockState2);
        when(mockTaskRegistry.getTasks()).thenReturn(Lists.newArrayList(mockTask, mockTask2));

        GlobalSplitInstallTask<List<GlobalSplitInstallSessionState>> task = globallyDynamicSplitInstallManager.getSessionStates();

        assertThat(task.getResult()).isEqualTo(Lists.newArrayList(mockState, mockState2));
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.KITKAT)
    public void whenSdkIsOlderThan21_cancelInstall_shouldFailWithApiUnavailable() {
        GlobalSplitInstallTask<Void> task = globallyDynamicSplitInstallManager.cancelInstall(0);

        assertThat(((GlobalSplitInstallException) task.getException()).getErrorCode()).isEqualTo(GlobalSplitInstallErrorCode.API_NOT_AVAILABLE);
    }

    @Test
    public void whenTaskIsNotPresent_cancelInstall_shouldFailWithSessionNotFound() {
        GlobalSplitInstallTask<Void> task = globallyDynamicSplitInstallManager.cancelInstall(0);

        assertThat(((GlobalSplitInstallException) task.getException()).getErrorCode()).isEqualTo(GlobalSplitInstallErrorCode.SESSION_NOT_FOUND);
    }

    @Test
    public void whenTaskIsPresent_cancelInstall_shouldCallCancelOnIt() {
        int sessionId = 1;
        InstallTask mockTask = mock(InstallTask.class);
        GlobalSplitInstallSessionState mockState = mock(GlobalSplitInstallSessionState.class);
        when(mockTask.getCurrentState()).thenReturn(mockState);
        when(mockTaskRegistry.findTaskBySessionId(sessionId)).thenReturn(mockTask);

        GlobalSplitInstallTask<Void> task = globallyDynamicSplitInstallManager.cancelInstall(sessionId);

        verify(mockTask).cancel(sessionId);
        assertThat(task.getResult()).isNull();
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.KITKAT)
    public void whenSdkIsOlderThan21_startInstall_shouldFailWithApiUnavailable() {
        GlobalSplitInstallTask<Integer> task = globallyDynamicSplitInstallManager.startInstall(GlobalSplitInstallRequest.newBuilder().build());

        assertThat(((GlobalSplitInstallException) task.getException()).getErrorCode()).isEqualTo(GlobalSplitInstallErrorCode.API_NOT_AVAILABLE);
    }

    @Test
    public void whenAllModulesAndLanguagesAreInstalled_startInstall_shouldReturnImmediately() {
        Map<String, ModuleConditions> installTimeFeatures = new HashMap<String, ModuleConditions>();
        installTimeFeatures.put("a", null);
        String[] onDemandFeatures = new String[]{"b"};
        when(mockGloballyDynamicBuildConfig.getInstallTimeFeatures()).thenReturn(installTimeFeatures);
        when(mockGloballyDynamicBuildConfig.getOnDemandFeatures()).thenReturn(onDemandFeatures);
        when(mockGlobalSplitInstallManager.getInstalledModules()).thenReturn(Sets.newHashSet("a", "b"));
        when(mockGlobalSplitInstallManager.getInstalledLanguages()).thenReturn(Sets.newHashSet("c", "d"));

        GlobalSplitInstallTask<Integer> task = globallyDynamicSplitInstallManager.startInstall(GlobalSplitInstallRequest.newBuilder()
                .addModule("a")
                .addModule("b")
                .addLanguage(new Locale("c", "c"))
                .addLanguage(new Locale("d", "d"))
                .build()
        );

        assertThat(task.getResult()).isEqualTo(0);
    }

    @Test
    public void whenModuleDoesNotExist_startInstall_shouldReturnModuleNotAvailable() {
        GlobalSplitInstallTask<Integer> task = globallyDynamicSplitInstallManager.startInstall(GlobalSplitInstallRequest.newBuilder()
                .addModule("a")
                .build()
        );

        assertThat(((GlobalSplitInstallException) task.getException()).getErrorCode()).isEqualTo(GlobalSplitInstallErrorCode.MODULE_UNAVAILABLE);
    }

    @Test
    public void whenEqualTaskIsRunning_startInstall_shouldReturnActiveSessionsExceeded() {
        when(mockGloballyDynamicBuildConfig.getOnDemandFeatures()).thenReturn(new String[]{"a"});
        InstallTask mockTask = mock(InstallTask.class);
        when(mockTask.getInstallRequest()).thenReturn(GlobalSplitInstallRequestInternal.newBuilder()
                .addModule("a")
                .addLanguage(new Locale("c", "c"))
                .build());
        when(mockTaskRegistry.getTasks()).thenReturn(Lists.newArrayList(mockTask));

        GlobalSplitInstallTask<Integer> task = globallyDynamicSplitInstallManager.startInstall(GlobalSplitInstallRequest.newBuilder()
                .addModule("a")
                .addLanguage(new Locale("c", "c"))
                .build()
        );

        assertThat(((GlobalSplitInstallException) task.getException()).getErrorCode()).isEqualTo(GlobalSplitInstallErrorCode.ACTIVE_SESSIONS_LIMIT_EXCEEDED);
    }
}