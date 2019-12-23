package com.jeppeman.globallydynamic.tasks;

/**
 * A listener that can be attached to a {@link GlobalSplitInstallTask} to be notified when a task
 * fails
 */
public interface OnGlobalSplitInstallFailureListener {
    /**
     * Invoked when the task that this listener is attached to fails
     *
     * @param e the cause of the failure
     */
    void onFailure(Exception e);
}
