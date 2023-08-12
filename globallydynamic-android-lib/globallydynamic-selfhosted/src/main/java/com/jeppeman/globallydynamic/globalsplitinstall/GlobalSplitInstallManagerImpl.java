package com.jeppeman.globallydynamic.globalsplitinstall;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.google.android.play.core.splitinstall.SplitInstallManagerFactory;
import com.jeppeman.globallydynamic.generated.GloballyDynamicBuildConfig;
import com.jeppeman.globallydynamic.net.HttpClient;
import com.jeppeman.globallydynamic.net.HttpClientFactory;
import com.jeppeman.globallydynamic.tasks.GlobalSplitInstallTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

class GlobalSplitInstallManagerImpl implements GlobalSplitInstallManager, InstallListenersProvider {
    private final Context context;
    private final Executor executor;
    private final GlobalSplitInstallManager delegate;
    private final GloballyDynamicConfigurationRepository globallyDynamicConfigurationRepository;
    private final GloballyDynamicBuildConfig globallyDynamicBuildConfig;
    private final TaskRegistry taskRegistry;
    private final Logger logger;
    private final ApplicationPatcher applicationPatcher;
    private final MissingSplitsManager missingSplitsManager;
    private final SignatureProvider signatureProvider;
    private final HttpClient httpClient;
    private final List<GlobalSplitInstallUpdatedListener> installListeners =
            new CopyOnWriteArrayList<GlobalSplitInstallUpdatedListener>(
                    new ArrayList<GlobalSplitInstallUpdatedListener>());

    private GlobalSplitInstallManagerImpl(
            @NonNull Context context,
            @NonNull GloballyDynamicBuildConfig globallyDynamicBuildConfig,
            @NonNull Executor executor,
            @NonNull GlobalSplitInstallManager delegate,
            @NonNull GLExtensionsExtractor glExtensionsExtractor,
            @NonNull Logger logger,
            @NonNull SignatureProvider signatureProvider
    ) {
        this(
                context,
                globallyDynamicBuildConfig,
                executor,
                delegate,
                glExtensionsExtractor,
                logger,
                TaskRegistryFactory.create(),
                ApplicationPatcherFactory.create(context, logger),
                signatureProvider,
                HttpClientFactory.builder()
                        .setConnectTimeout(globallyDynamicBuildConfig.getDownloadConnectTimeout())
                        .setReadTimeout(globallyDynamicBuildConfig.getDownloadReadTimeout())
                        .setLogger(LoggerFactory.createHttpLogger(logger))
                        .build()
        );
    }

    private GlobalSplitInstallManagerImpl(
            @NonNull Context context,
            @NonNull GloballyDynamicBuildConfig globallyDynamicBuildConfig,
            @NonNull Executor executor,
            @NonNull GlobalSplitInstallManager delegate,
            @NonNull GLExtensionsExtractor glExtensionsExtractor,
            @NonNull Logger logger,
            @NonNull TaskRegistry taskRegistry,
            @NonNull ApplicationPatcher applicationPatcher,
            @NonNull SignatureProvider signatureProvider,
            @NonNull HttpClient httpClient
    ) {
        this(
                context,
                globallyDynamicBuildConfig,
                executor,
                delegate,
                GloballyDynamicConfigurationRepositoryFactory.create(
                        context,
                        glExtensionsExtractor,
                        globallyDynamicBuildConfig,
                        logger
                ),
                taskRegistry,
                logger,
                applicationPatcher,
                signatureProvider,
                httpClient
        );
    }

    private GlobalSplitInstallManagerImpl(
            @NonNull Context context,
            @NonNull GloballyDynamicBuildConfig globallyDynamicBuildConfig,
            @NonNull Executor executor,
            @NonNull GlobalSplitInstallManager delegate,
            @NonNull GloballyDynamicConfigurationRepository globallyDynamicConfigurationRepository,
            @NonNull TaskRegistry taskRegistry,
            @NonNull Logger logger,
            @NonNull ApplicationPatcher applicationPatcher,
            @NonNull SignatureProvider signatureProvider,
            @NonNull HttpClient httpClient
    ) {
        this(
                context,
                executor,
                delegate,
                globallyDynamicConfigurationRepository,
                globallyDynamicBuildConfig,
                taskRegistry,
                logger,
                applicationPatcher,
                MissingSplitsManagerFactory.create(
                        globallyDynamicBuildConfig,
                        globallyDynamicConfigurationRepository,
                        logger
                ),
                signatureProvider,
                httpClient
        );
    }

    GlobalSplitInstallManagerImpl(
            @NonNull Context context,
            @NonNull Executor executor,
            @NonNull GlobalSplitInstallManager delegate,
            @NonNull GloballyDynamicConfigurationRepository globallyDynamicConfigurationRepository,
            @NonNull GloballyDynamicBuildConfig globallyDynamicBuildConfig,
            @NonNull TaskRegistry taskRegistry,
            @NonNull Logger logger,
            @NonNull ApplicationPatcher applicationPatcher,
            @NonNull MissingSplitsManager missingSplitsManager,
            @NonNull SignatureProvider signatureProvider,
            @NonNull HttpClient httpClient
    ) {
        this.context = context.getApplicationContext();
        this.executor = executor;
        this.delegate = delegate;
        this.globallyDynamicConfigurationRepository = globallyDynamicConfigurationRepository;
        this.globallyDynamicBuildConfig = globallyDynamicBuildConfig;
        this.taskRegistry = taskRegistry;
        this.logger = logger;
        this.applicationPatcher = applicationPatcher;
        this.missingSplitsManager = missingSplitsManager;
        this.signatureProvider = signatureProvider;
        this.httpClient = httpClient;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            logger.i("Current API level is " + Build.VERSION.SDK_INT + ", GloballyDynamic will" +
                    " not work as it is only compatible with 21 and above");
        }
    }

    GlobalSplitInstallManagerImpl(
            @NonNull Context context,
            @NonNull GloballyDynamicBuildConfig globallyDynamicBuildConfig) {
        this(
                context,
                globallyDynamicBuildConfig,
                ExecutorFactory.create(),
                new SelfHostedGlobalSplitInstallManager(SplitInstallManagerFactory.create(context)),
                GLExtensionsExtractorFactory.create(context),
                LoggerFactory.create(),
                SignatureProviderFactory.create(context)
        );
    }

    private boolean hasFeaturesAndLanguages(GlobalSplitInstallRequestInternal request) {
        if (request.isUninstall()) return false;

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

        return hasAllModules && hasAllLanguages;
    }

    private boolean modulesAreValid(GlobalSplitInstallRequestInternal request) {
        Set<String> installTimeFeatures = globallyDynamicBuildConfig.getInstallTimeFeatures().keySet();
        List<String> onDemandFeatures = Arrays.asList(globallyDynamicBuildConfig.getOnDemandFeatures());
        for (String module : request.getModuleNames()) {
            if (!onDemandFeatures.contains(module) && !installTimeFeatures.contains(module)) {
                return false;
            }
        }

        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private boolean requestIsRunning(GlobalSplitInstallRequestInternal request) {
        boolean runningTasksExist = !taskRegistry.getTasks().isEmpty();
        for (InstallTask task : taskRegistry.getTasks()) {
            for (String module : request.getModuleNames()) {
                if (!task.getInstallRequest().getModuleNames().contains(module)) {
                    return false;
                }
            }

            for (Locale language : request.getLanguages()) {
                if (!task.getInstallRequest().getLanguages().contains(language)) {
                    return false;
                }
            }
        }

        return runningTasksExist;
    }

    private GlobalSplitInstallTask<Integer> startInstall(GlobalSplitInstallRequestInternal request) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return GloballyDynamicTasks.empty(GlobalSplitInstallExceptionFactory.create(
                    GlobalSplitInstallErrorCode.API_NOT_AVAILABLE));
        }

        if (!modulesAreValid(request)) {
            return GloballyDynamicTasks.empty(GlobalSplitInstallExceptionFactory.create(
                    GlobalSplitInstallErrorCode.MODULE_UNAVAILABLE));
        }

        if (requestIsRunning(request)) {
            return GloballyDynamicTasks.empty(GlobalSplitInstallExceptionFactory.create(
                    GlobalSplitInstallErrorCode.ACTIVE_SESSIONS_LIMIT_EXCEEDED));
        }

        if (!request.shouldIncludeMissingSplits() && hasFeaturesAndLanguages(request)) {
            return GloballyDynamicTasks.empty(0);
        } else {
            InstallTask task = GloballyDynamicTasks.create(
                    globallyDynamicConfigurationRepository,
                    context,
                    this,
                    request,
                    executor,
                    logger,
                    signatureProvider,
                    applicationPatcher,
                    taskRegistry,
                    httpClient
            );
            taskRegistry.registerTask(task);
            return task;
        }
    }

    @Override
    public List<GlobalSplitInstallUpdatedListener> getListeners() {
        return installListeners;
    }

    @Override
    public void registerListener(GlobalSplitInstallUpdatedListener listener) {
        installListeners.add(listener);
    }

    @Override
    public void unregisterListener(GlobalSplitInstallUpdatedListener listener) {
        installListeners.remove(listener);
    }

    @Override
    public GlobalSplitInstallTask<Integer> startInstall(GlobalSplitInstallRequest request) {
        return startInstall(GlobalSplitInstallRequestInternal.fromExternal(request));
    }

    @Override
    public GlobalSplitInstallTask<Integer> installMissingSplits() {
        GlobalSplitInstallRequestInternal request = missingSplitsManager.getMissingSplitsRequest(
                getInstalledModules());
        if (!request.shouldIncludeMissingSplits() && request.getModuleNames().isEmpty()) {
            return GloballyDynamicTasks.empty(0);
        } else {
            return startInstall(request);
        }
    }

    @Override
    public GlobalSplitInstallTask<Void> cancelInstall(int sessionId) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return GloballyDynamicTasks.empty(GlobalSplitInstallExceptionFactory.create(
                    GlobalSplitInstallErrorCode.API_NOT_AVAILABLE));
        }

        InstallTask maybeTask = taskRegistry.findTaskBySessionId(sessionId);
        if (maybeTask != null) {
            maybeTask.cancel(sessionId);
            Void aVoid = null;
            return GloballyDynamicTasks.empty(aVoid);
        } else {
            return GloballyDynamicTasks.empty(GlobalSplitInstallExceptionFactory.create(
                    GlobalSplitInstallErrorCode.SESSION_NOT_FOUND));
        }
    }

    @Override
    public GlobalSplitInstallTask<Integer> startUninstall(List<String> moduleNames) {
        GlobalSplitInstallRequestInternal.Builder request =
                GlobalSplitInstallRequestInternal.newBuilder().isUninstall(true);
        for (String moduleName : moduleNames) request.addModule(moduleName);
        return startInstall(request.build());
    }

    @Override
    public GlobalSplitInstallTask<List<GlobalSplitInstallSessionState>> getSessionStates() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return GloballyDynamicTasks.empty(GlobalSplitInstallExceptionFactory.create(
                    GlobalSplitInstallErrorCode.API_NOT_AVAILABLE));
        }

        List<GlobalSplitInstallSessionState> states = new LinkedList<GlobalSplitInstallSessionState>();
        for (InstallTask task : taskRegistry.getTasks()) {
            states.add(task.getCurrentState());
        }

        return GloballyDynamicTasks.empty(states);
    }

    @Override
    public GlobalSplitInstallTask<GlobalSplitInstallSessionState> getSessionState(int sessionId) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return GloballyDynamicTasks.empty(GlobalSplitInstallExceptionFactory.create(
                    GlobalSplitInstallErrorCode.API_NOT_AVAILABLE));
        }

        InstallTask maybeTask = taskRegistry.findTaskBySessionId(sessionId);
        if (maybeTask != null) {
            return GloballyDynamicTasks.empty(maybeTask.getCurrentState());
        } else {
            return GloballyDynamicTasks.empty(GlobalSplitInstallExceptionFactory.create(
                    GlobalSplitInstallErrorCode.SESSION_NOT_FOUND));
        }
    }

    @Override
    public boolean startConfirmationDialogForResult(GlobalSplitInstallSessionState sessionState, Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return false;
        }

        InstallTask maybeTask = taskRegistry.findTaskBySessionId(sessionState.sessionId());
        if (maybeTask != null) {
            Intent intent = new Intent(context, UserConfirmationActivity.class);
            activity.startActivityForResult(intent, requestCode);
            taskRegistry.unregisterTask(maybeTask);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public GlobalSplitInstallTask<Void> deferredUninstall(List<String> moduleNames) {
        return delegate.deferredUninstall(moduleNames);
    }

    @Override
    public GlobalSplitInstallTask<Void> deferredInstall(List<String> moduleNames) {
        return delegate.deferredInstall(moduleNames);
    }

    @Override
    public GlobalSplitInstallTask<Void> deferredLanguageInstall(List<Locale> languages) {
        return delegate.deferredLanguageInstall(languages);
    }

    @Override
    public GlobalSplitInstallTask<Void> deferredLanguageUninstall(List<Locale> languages) {
        return delegate.deferredLanguageUninstall(languages);
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

        installedLanguages.addAll(delegate.getInstalledLanguages());

        return installedLanguages;
    }

    @Override
    public Set<String> getInstalledModules() {
        return delegate.getInstalledModules();
    }
}