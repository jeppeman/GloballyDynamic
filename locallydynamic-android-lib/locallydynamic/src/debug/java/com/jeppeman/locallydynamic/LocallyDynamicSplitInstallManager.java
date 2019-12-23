package com.jeppeman.locallydynamic;

import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.google.android.play.core.splitinstall.SplitInstallException;
import com.google.android.play.core.splitinstall.SplitInstallManager;
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory;
import com.google.android.play.core.splitinstall.SplitInstallRequest;
import com.google.android.play.core.splitinstall.SplitInstallSessionState;
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener;
import com.google.android.play.core.splitinstall.model.SplitInstallErrorCode;
import com.google.android.play.core.tasks.OnCompleteListener;
import com.google.android.play.core.tasks.Task;
import com.google.android.play.core.tasks.Tasks;
import com.jeppeman.locallydynamic.generated.LocallyDynamicBuildConfig;
import com.jeppeman.locallydynamic.net.HttpClientFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

class LocallyDynamicSplitInstallManagerImpl implements SplitInstallManager {
    private final Context context;
    private final Executor executor;
    private final SplitInstallManager splitInstallManager;
    private final LocallyDynamicBuildConfig locallyDynamicBuildConfig;
    private final GLExtensionsExtractor glExtensionsExtractor;
    private final LocallyDynamicApi locallyDynamicApi;
    private final LocallyDynamicConfigurationRepository locallyDynamicConfigurationRepository;
    private final TaskRegistry taskRegistry;
    private final Logger logger;
    private final List<SplitInstallStateUpdatedListener> installListeners =
            new CopyOnWriteArrayList<SplitInstallStateUpdatedListener>(
                    new ArrayList<SplitInstallStateUpdatedListener>());
    @VisibleForTesting
    final AtomicInteger taskCounter = new AtomicInteger();

    private LocallyDynamicSplitInstallManagerImpl(
            @NonNull Context context,
            @NonNull LocallyDynamicBuildConfig locallyDynamicBuildConfig,
            @NonNull Executor executor,
            @NonNull SplitInstallManager splitInstallManager,
            @NonNull GLExtensionsExtractor glExtensionsExtractor,
            @NonNull Logger logger) {
        this(
                context,
                locallyDynamicBuildConfig,
                executor,
                splitInstallManager,
                glExtensionsExtractor,
                LocallyDynamicApiFactory.create(
                        HttpClientFactory.builder()
                                .setLogger(LoggerFactory.createHttpLogger(logger))
                                .build(),
                        locallyDynamicBuildConfig
                ),
                logger
        );
    }


    private LocallyDynamicSplitInstallManagerImpl(
            @NonNull Context context,
            @NonNull LocallyDynamicBuildConfig locallyDynamicBuildConfig,
            @NonNull Executor executor,
            @NonNull SplitInstallManager splitInstallManager,
            @NonNull GLExtensionsExtractor glExtensionsExtractor,
            @NonNull LocallyDynamicApi locallyDynamicApi,
            @NonNull Logger logger) {
        this(
                context,
                locallyDynamicBuildConfig,
                executor,
                splitInstallManager,
                glExtensionsExtractor,
                locallyDynamicApi,
                LocallyDynamicConfigurationRepositoryFactory.create(
                        context,
                        glExtensionsExtractor,
                        locallyDynamicBuildConfig,
                        locallyDynamicApi,
                        logger
                ),
                TaskRegistryFactory.create(),
                logger
        );
    }

    LocallyDynamicSplitInstallManagerImpl(
            @NonNull Context context,
            @NonNull LocallyDynamicBuildConfig locallyDynamicBuildConfig,
            @NonNull Executor executor,
            @NonNull SplitInstallManager splitInstallManager,
            @NonNull GLExtensionsExtractor glExtensionsExtractor,
            @NonNull LocallyDynamicApi locallyDynamicApi,
            @NonNull LocallyDynamicConfigurationRepository locallyDynamicConfigurationRepository,
            @NonNull TaskRegistry taskRegistry,
            @NonNull Logger logger) {
        this.context = context;
        this.locallyDynamicBuildConfig = locallyDynamicBuildConfig;
        this.executor = executor;
        this.splitInstallManager = splitInstallManager;
        this.glExtensionsExtractor = glExtensionsExtractor;
        this.locallyDynamicApi = locallyDynamicApi;
        this.locallyDynamicConfigurationRepository = locallyDynamicConfigurationRepository;
        this.taskRegistry = taskRegistry;
        this.logger = logger;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            logger.i("Current API level is " + Build.VERSION.SDK_INT + ", LocallyDynamic will" +
                    " not work as it is only compatible with 21 and above");
        }
    }

    LocallyDynamicSplitInstallManagerImpl(
            @NonNull Context context,
            @NonNull LocallyDynamicBuildConfig locallyDynamicBuildConfig,
            @NonNull Logger logger) {
        this(
                context,
                locallyDynamicBuildConfig,
                ExecutorFactory.create(),
                SplitInstallManagerFactory.create(context),
                GLExtensionsExtractorFactory.create(context),
                logger
        );
    }

    @Override
    public void registerListener(SplitInstallStateUpdatedListener listener) {
        installListeners.add(listener);
    }

    @Override
    public void unregisterListener(SplitInstallStateUpdatedListener listener) {
        installListeners.remove(listener);
    }

    @Override
    public Task<Integer> startInstall(SplitInstallRequest request) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return Tasks.a(new SplitInstallException(SplitInstallErrorCode.API_NOT_AVAILABLE));
        }

        boolean hasAllModules = true;
        boolean hasAllLanguages = true;
        for (String module : request.getModuleNames()) {
            if (!getInstalledModules().contains(module)) {
                hasAllModules = false;
                break;
            }
        }

        for (Locale locale : request.getLanguages()) {
            if (!getInstalledLanguages().contains(locale.getLanguage())) {
                hasAllLanguages = false;
                break;
            }
        }

        if (hasAllModules && hasAllLanguages) {
            return Tasks.a(0);
        } else {
            final LocallyDynamicInstallTask locallyDynamicInstallTask = LocallyDynamicInstallTask.create(
                    locallyDynamicConfigurationRepository,
                    context,
                    installListeners,
                    request,
                    executor,
                    logger,
                    taskCounter.incrementAndGet()
            );

            taskRegistry.registerTask(locallyDynamicInstallTask);

            locallyDynamicInstallTask.addOnCompleteListener(new OnCompleteListener<Integer>() {
                @Override
                public void onComplete(Task<Integer> task) {
                    taskRegistry.unregisterTask(locallyDynamicInstallTask);
                }
            });

            locallyDynamicInstallTask.start();

            return locallyDynamicInstallTask;
        }
    }

    @Override
    public Task<Void> cancelInstall(int sessionId) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return Tasks.a(new SplitInstallException(SplitInstallErrorCode.API_NOT_AVAILABLE));
        }

        LocallyDynamicInstallTask maybeTask = taskRegistry.findTaskBySessionId(sessionId);
        if (maybeTask != null) {
            maybeTask.cancel(sessionId);
            Void aVoid = null;
            return Tasks.a(aVoid);
        } else {
            return Tasks.a(new SplitInstallException(SplitInstallErrorCode.INVALID_REQUEST));
        }
    }

    @Override
    public Task<List<SplitInstallSessionState>> getSessionStates() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return Tasks.a(new SplitInstallException(SplitInstallErrorCode.API_NOT_AVAILABLE));
        }

        List<SplitInstallSessionState> states = new LinkedList<SplitInstallSessionState>();
        for (LocallyDynamicInstallTask task : taskRegistry.getTasks()) {
            states.add(task.getCurrentState());
        }

        return Tasks.a(states);
    }

    @Override
    public Task<SplitInstallSessionState> getSessionState(int sessionId) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return Tasks.a(new SplitInstallException(SplitInstallErrorCode.API_NOT_AVAILABLE));
        }

        LocallyDynamicInstallTask maybeTask = taskRegistry.findTaskBySessionId(sessionId);
        if (maybeTask != null) {
            return Tasks.a(maybeTask.getCurrentState());
        } else {
            return Tasks.a(new SplitInstallException(SplitInstallErrorCode.INVALID_REQUEST));
        }
    }

    @Override
    public boolean startConfirmationDialogForResult(SplitInstallSessionState sessionState, Activity activity, int requestCode) throws IntentSender.SendIntentException {
        return splitInstallManager.startConfirmationDialogForResult(sessionState, activity, requestCode);
    }

    @Override
    public Task<Void> deferredUninstall(List<String> moduleNames) {
        return splitInstallManager.deferredUninstall(moduleNames);
    }

    @Override
    public Task<Void> deferredInstall(List<String> moduleNames) {
        return splitInstallManager.deferredInstall(moduleNames);
    }

    @Override
    public Task<Void> deferredLanguageInstall(List<Locale> languages) {
        return splitInstallManager.deferredLanguageInstall(languages);
    }

    @Override
    public Task<Void> deferredLanguageUninstall(List<Locale> languages) {
        return splitInstallManager.deferredLanguageUninstall(languages);
    }

    @Override
    public Set<String> getInstalledLanguages() {
        Set<String> installedLanguages = new HashSet<String>();
        for (String module : getInstalledModules()) {
            String[] splitOnDash = module.split("-");
            if (splitOnDash.length > 1) {
                String apkSuffix = splitOnDash[1];
                if (apkSuffix.length() == 2) { // Length of lang code
                    installedLanguages.add(apkSuffix);
                }
            }
        }

        installedLanguages.addAll(splitInstallManager.getInstalledLanguages());

        return installedLanguages;
    }

    @Override
    public Set<String> getInstalledModules() {
        return splitInstallManager.getInstalledModules();
    }
}