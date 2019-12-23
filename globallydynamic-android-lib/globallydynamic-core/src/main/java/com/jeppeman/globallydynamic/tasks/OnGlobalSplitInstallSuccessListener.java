package com.jeppeman.globallydynamic.tasks;

/**
 * A listener that can be attached to a {@link GlobalSplitInstallTask} to be notified when a task
 * succeeds
 *
 * @param <TResult> the type of the result of the task
 */
public interface OnGlobalSplitInstallSuccessListener<TResult> {
    /**
     * Invoked when the task that this listener is attached to succeeds
     *
     * @param result the result of the task
     */
    void onSuccess(TResult result);
}
