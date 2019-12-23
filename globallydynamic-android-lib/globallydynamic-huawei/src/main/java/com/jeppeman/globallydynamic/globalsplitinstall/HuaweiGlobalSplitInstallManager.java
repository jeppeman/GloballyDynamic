package com.jeppeman.globallydynamic.globalsplitinstall;

import android.app.Activity;
import android.content.IntentSender;

import com.huawei.hms.feature.install.FeatureInstallManager;
import com.huawei.hms.feature.listener.InstallStateListener;
import com.huawei.hms.feature.model.InstallState;
import com.huawei.hms.feature.tasks.FeatureTask;
import com.jeppeman.globallydynamic.tasks.GlobalSplitInstallTask;
import com.jeppeman.globallydynamic.tasks.GlobalSplitInstallTasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

class HuaweiGlobalSplitInstallManager implements GlobalSplitInstallManager {
    private final FeatureInstallManager delegate;
    private final Map<GlobalSplitInstallUpdatedListener, InstallStateListener> listeners =
            new HashMap<GlobalSplitInstallUpdatedListener, InstallStateListener>();

    HuaweiGlobalSplitInstallManager(FeatureInstallManager delegate) {
        this.delegate = delegate;
    }

    @Override
    public void registerListener(final GlobalSplitInstallUpdatedListener listener) {
        InstallStateListener installStateListener = new InstallStateListener() {
            @Override
            public void onStateUpdate(InstallState installState) {
                listener.onStateUpdate(HuaweiGlobalSplitInstallSessionStateMapper.toGlobalSplitInstallSessionState(installState));
            }
        };

        listeners.put(listener, installStateListener);

        delegate.registerInstallListener(installStateListener);
    }

    @Override
    public void unregisterListener(GlobalSplitInstallUpdatedListener listener) {
        InstallStateListener installStateListener = listeners.get(listener);
        if (installStateListener != null) {
            delegate.unregisterInstallListener(installStateListener);
            listeners.remove(listener);
        }
    }

    @Override
    public boolean startConfirmationDialogForResult(GlobalSplitInstallSessionState sessionState, Activity activity, int requestCode) throws IntentSender.SendIntentException {
        return delegate.triggerUserConfirm(HuaweiGlobalSplitInstallSessionStateMapper.toInstallState(sessionState), activity, requestCode);
    }

    @Override
    public GlobalSplitInstallTask<Integer> startInstall(GlobalSplitInstallRequest request) {
        FeatureTask<Integer> featureTask = delegate.installFeature(
                HuaweiGlobalSplitInstallRequestMapper.toFeatureInstallRequest(request));
        return new HuaweiGlobalSplitInstallTask<Integer, Integer>(featureTask);
    }

    @Override
    public GlobalSplitInstallTask<Integer> installMissingSplits() {
        return GlobalSplitInstallTasks.empty(0);
    }

    @Override
    public GlobalSplitInstallTask<Void> cancelInstall(int sessionId) {
        return new HuaweiGlobalSplitInstallTask<Void, Void>(delegate.abortInstallFeature(sessionId));
    }

    @Override
    public GlobalSplitInstallTask<Void> deferredInstall(List<String> moduleNames) {
        return new HuaweiGlobalSplitInstallTask<Void, Void>(delegate.delayedInstallFeature(moduleNames));
    }

    @Override
    public GlobalSplitInstallTask<Void> deferredUninstall(List<String> moduleNames) {
        return new HuaweiGlobalSplitInstallTask<Void, Void>(delegate.delayedUninstallFeature(moduleNames));
    }

    @Override
    public GlobalSplitInstallTask<Void> deferredLanguageInstall(List<Locale> languages) {
        Void aVoid = null;
        return GlobalSplitInstallTasks.empty(aVoid);
    }

    @Override
    public GlobalSplitInstallTask<Void> deferredLanguageUninstall(List<Locale> languages) {
        Void aVoid = null;
        return GlobalSplitInstallTasks.empty(aVoid);
    }

    @Override
    public GlobalSplitInstallTask<GlobalSplitInstallSessionState> getSessionState(int sessionId) {
        return new HuaweiGlobalSplitInstallTask<InstallState, GlobalSplitInstallSessionState>(
                delegate.getInstallState(sessionId),
                new HuaweiGlobalSplitInstallResultMapper<InstallState, GlobalSplitInstallSessionState>() {
                    @Override
                    public GlobalSplitInstallSessionState map(InstallState from) {
                        return HuaweiGlobalSplitInstallSessionStateMapper.toGlobalSplitInstallSessionState(from);
                    }
                });
    }

    @Override
    public GlobalSplitInstallTask<List<GlobalSplitInstallSessionState>> getSessionStates() {
        return new HuaweiGlobalSplitInstallTask<List<InstallState>, List<GlobalSplitInstallSessionState>>(
                delegate.getAllInstallStates(),
                new HuaweiGlobalSplitInstallResultMapper<List<InstallState>, List<GlobalSplitInstallSessionState>>() {
                    @Override
                    public List<GlobalSplitInstallSessionState> map(List<InstallState> from) {
                        List<GlobalSplitInstallSessionState> ret = new ArrayList<GlobalSplitInstallSessionState>(from.size());
                        for (InstallState installState : from) {
                            ret.add(HuaweiGlobalSplitInstallSessionStateMapper.toGlobalSplitInstallSessionState(installState));
                        }
                        return ret;
                    }
                });
    }

    @Override
    public Set<String> getInstalledLanguages() {
        return new HashSet<String>();
    }

    @Override
    public Set<String> getInstalledModules() {
        return delegate.getAllInstalledModules();
    }
}
