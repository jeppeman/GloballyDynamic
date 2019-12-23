package com.jeppeman.locallydynamic;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;

class TaskRegistryFactory {
    static TaskRegistry create() {
        return new TaskRegistryImpl();
    }
}

interface TaskRegistry {
    List<LocallyDynamicInstallTask> getTasks();
    void registerTask(LocallyDynamicInstallTask task);
    void unregisterTask(LocallyDynamicInstallTask task);
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    LocallyDynamicInstallTask findTaskBySessionId(int sessionId);
}

class TaskRegistryImpl implements TaskRegistry {
    private List<LocallyDynamicInstallTask> tasks = new ArrayList<LocallyDynamicInstallTask>();

    @Override
    public List<LocallyDynamicInstallTask> getTasks() {
        return tasks;
    }

    @Override
    public void registerTask(LocallyDynamicInstallTask task) {
        tasks.add(task);
    }

    @Override
    public void unregisterTask(LocallyDynamicInstallTask task) {
        tasks.remove(task);
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public LocallyDynamicInstallTask findTaskBySessionId(int sessionId) {
        for (LocallyDynamicInstallTask task : tasks) {
            if (task.getCurrentState().sessionId() == sessionId) {
                return task;
            }
        }

        return null;
    }
}