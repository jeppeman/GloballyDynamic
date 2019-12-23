package com.jeppeman.globallydynamic.globalsplitinstall;

import android.content.Context;
import android.os.Build;

import com.jeppeman.globallydynamic.net.HttpClient;
import com.jeppeman.globallydynamic.tasks.GlobalSplitInstallTask;
import com.jeppeman.globallydynamic.tasks.GlobalSplitInstallTasks;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

class GloballyDynamicTasks {
    static <TResult> GlobalSplitInstallTask<TResult> empty(TResult result) {
        return GlobalSplitInstallTasks.empty(result);
    }

    static <TResult> GlobalSplitInstallTask<TResult> empty(Exception exception) {
        return GlobalSplitInstallTasks.empty(exception);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    static InstallTask create(
            @NonNull GloballyDynamicConfigurationRepository globallyDynamicConfigurationRepository,
            @NonNull Context context,
            @NonNull InstallListenersProvider installListenersProvider,
            @NonNull GlobalSplitInstallRequestInternal splitInstallRequest,
            @NonNull Executor executor,
            @NonNull Logger logger,
            @NonNull SignatureProvider signatureProvider,
            @NonNull ApplicationPatcher applicationPatcher,
            @NonNull TaskRegistry taskRegistry,
            @NonNull HttpClient httpClient) {
        return new InstallTaskImpl(
                globallyDynamicConfigurationRepository,
                context,
                installListenersProvider,
                splitInstallRequest,
                executor,
                logger,
                signatureProvider,
                applicationPatcher,
                taskRegistry,
                httpClient
        );
    }
}

