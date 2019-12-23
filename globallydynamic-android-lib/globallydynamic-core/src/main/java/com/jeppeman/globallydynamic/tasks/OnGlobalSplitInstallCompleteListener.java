package com.jeppeman.globallydynamic.tasks;

/**
 * A listener that can be attached to a {@link GlobalSplitInstallTask} to be notified when a task
 * completes
 *
 * @param <TResult> the type of the result of the task
 */
public interface OnGlobalSplitInstallCompleteListener<TResult> {
    /**
     * Invoked when the task this listener is attached to completes
     *
     * @param task the task this listener is attached to
     */
    void onComplete(GlobalSplitInstallTask<TResult> task);
}
