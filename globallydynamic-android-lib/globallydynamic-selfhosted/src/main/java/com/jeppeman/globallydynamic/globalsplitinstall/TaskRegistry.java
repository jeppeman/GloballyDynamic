package com.jeppeman.globallydynamic.globalsplitinstall;

import android.os.Build;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import androidx.annotation.RequiresApi;

class TaskRegistryFactory {
    static TaskRegistry create() {
        return new TaskRegistryImpl();
    }
}

interface TaskRegistry {
    List<InstallTask> getTasks();
    void registerTask(InstallTask task);
    void unregisterTask(InstallTask task);
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    InstallTask findTaskBySessionId(int sessionId);
}

class TaskRegistryImpl implements TaskRegistry {
    private List<InstallTask> tasks = new CopyOnWriteArrayList<InstallTask>();

    @Override
    public List<InstallTask> getTasks() {
        return tasks;
    }

    @Override
    public void registerTask(InstallTask task) {
        tasks.add(task);
    }

    @Override
    public void unregisterTask(InstallTask task) {
        tasks.remove(task);
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public InstallTask findTaskBySessionId(int sessionId) {
        for (InstallTask task : tasks) {
            if (task.getCurrentState().sessionId() == sessionId) {
                return task;
            }
        }

        return null;
    }
}