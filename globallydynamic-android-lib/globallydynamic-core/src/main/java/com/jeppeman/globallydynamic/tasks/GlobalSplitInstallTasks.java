package com.jeppeman.globallydynamic.tasks;

/**
 * Factory for {@link GlobalSplitInstallTask}s that merely wraps a result or an exception
 */
public class GlobalSplitInstallTasks {
    /**
     * Creates a {@link GlobalSplitInstallTask} that wraps the provided result
     *
     * @param result the result to wrap
     * @param <TResult> the type of the result
     * @return a new {@link GlobalSplitInstallTask} wrapping the provided result
     */
    public static <TResult> GlobalSplitInstallTask<TResult> empty(TResult result) {
        return new GlobalSplitInstallEmptyTask<TResult>(result);
    }

    /**
     * Creates a {@link GlobalSplitInstallTask} that wraps the provided exception
     *
     * @param exception the exception to wrap
     * @param <TResult> the type of the result
     * @return a new {@link GlobalSplitInstallTask} wrapping the provided exception
     */
    public static <TResult> GlobalSplitInstallTask<TResult> empty(Exception exception) {
        return new GlobalSplitInstallEmptyTask<TResult>(exception);
    }
}

