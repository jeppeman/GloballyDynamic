package com.jeppeman.globallydynamic.globalsplitinstall;

import android.app.Activity;
import android.content.IntentSender;

import com.google.android.play.core.splitinstall.SplitInstallManager;
import com.google.android.play.core.splitinstall.SplitInstallSessionState;
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener;
import com.google.android.play.core.tasks.Task;
import com.jeppeman.globallydynamic.tasks.GlobalSplitInstallTask;
import com.jeppeman.globallydynamic.tasks.GlobalSplitInstallTasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

class GPlayGlobalSplitInstallManager implements GlobalSplitInstallManager {
    private final SplitInstallManager delegate;
    private final Map<GlobalSplitInstallUpdatedListener, SplitInstallStateUpdatedListener> listeners =
            new HashMap<GlobalSplitInstallUpdatedListener, SplitInstallStateUpdatedListener>();

    GPlayGlobalSplitInstallManager(SplitInstallManager delegate) {
        this.delegate = delegate;
    }

    @Override
    public void registerListener(final GlobalSplitInstallUpdatedListener listener) {
        SplitInstallStateUpdatedListener installStateListener = new SplitInstallStateUpdatedListener() {
            @Override
            public void onStateUpdate(SplitInstallSessionState installState) {
                listener.onStateUpdate(GPlayGlobalSplitInstallSessionStateMapper.toGlobalSplitInstallSessionState(installState));
            }
        };

        listeners.put(listener, installStateListener);

        delegate.registerListener(installStateListener);
    }

    @Override
    public void unregisterListener(GlobalSplitInstallUpdatedListener listener) {
        SplitInstallStateUpdatedListener installStateListener = listeners.get(listener);
        if (installStateListener != null) {
            delegate.unregisterListener(installStateListener);
            listeners.remove(listener);
        }
    }

    @Override
    public boolean startConfirmationDialogForResult(GlobalSplitInstallSessionState sessionState, Activity activity, int requestCode) throws IntentSender.SendIntentException {
        return delegate.startConfirmationDialogForResult(GPlayGlobalSplitInstallSessionStateMapper.toInstallState(sessionState), activity, requestCode);
    }

    @Override
    public GlobalSplitInstallTask<Integer> startInstall(GlobalSplitInstallRequest request) {
        Task<Integer> featureTask = delegate.startInstall(
                GPlayGlobalSplitInstallRequestMapper.toSplitInstallRequest(request));
        return new GPlayGlobalSplitInstallTask<Integer, Integer>(featureTask);
    }

    @Override
    public GlobalSplitInstallTask<Integer> installMissingSplits() {
        return GlobalSplitInstallTasks.empty(0);
    }

    @Override
    public GlobalSplitInstallTask<Void> cancelInstall(int sessionId) {
        return new GPlayGlobalSplitInstallTask<Void, Void>(delegate.cancelInstall(sessionId));
    }

    @Override
    public GlobalSplitInstallTask<Void> deferredInstall(List<String> moduleNames) {
        return new GPlayGlobalSplitInstallTask<Void, Void>(delegate.deferredInstall(moduleNames));
    }

    @Override
    public GlobalSplitInstallTask<Void> deferredUninstall(List<String> moduleNames) {
        return new GPlayGlobalSplitInstallTask<Void, Void>(delegate.deferredUninstall(moduleNames));
    }

    @Override
    public GlobalSplitInstallTask<Void> deferredLanguageInstall(List<Locale> languages) {
        return new GPlayGlobalSplitInstallTask<Void, Void>(delegate.deferredLanguageInstall(languages));
    }

    @Override
    public GlobalSplitInstallTask<Void> deferredLanguageUninstall(List<Locale> languages) {
        return new GPlayGlobalSplitInstallTask<Void, Void>(delegate.deferredLanguageUninstall(languages));
    }

    @Override
    public GlobalSplitInstallTask<GlobalSplitInstallSessionState> getSessionState(int sessionId) {
        return new GPlayGlobalSplitInstallTask<SplitInstallSessionState, GlobalSplitInstallSessionState>(
                delegate.getSessionState(sessionId),
                new GPlayGlobalSplitInstallResultMapper<SplitInstallSessionState, GlobalSplitInstallSessionState>() {
                    @Override
                    public GlobalSplitInstallSessionState map(SplitInstallSessionState from) {
                        return GPlayGlobalSplitInstallSessionStateMapper.toGlobalSplitInstallSessionState(from);
                    }
                });
    }

    @Override
    public GlobalSplitInstallTask<List<GlobalSplitInstallSessionState>> getSessionStates() {
        return new GPlayGlobalSplitInstallTask<List<SplitInstallSessionState>, List<GlobalSplitInstallSessionState>>(
                delegate.getSessionStates(),
                new GPlayGlobalSplitInstallResultMapper<List<SplitInstallSessionState>, List<GlobalSplitInstallSessionState>>() {
                    @Override
                    public List<GlobalSplitInstallSessionState> map(List<SplitInstallSessionState> from) {
                        List<GlobalSplitInstallSessionState> ret = new ArrayList<GlobalSplitInstallSessionState>(from.size());
                        for (SplitInstallSessionState installState : from) {
                            ret.add(GPlayGlobalSplitInstallSessionStateMapper.toGlobalSplitInstallSessionState(installState));
                        }
                        return ret;
                    }
                });
    }

    @Override
    public Set<String> getInstalledLanguages() {
        return delegate.getInstalledLanguages();
    }

    @Override
    public Set<String> getInstalledModules() {
        return delegate.getInstalledModules();
    }
}
