package com.jeppeman.globallydynamic.tasks;


import java.util.concurrent.Executor;

/**
 * A representation of an asynchronous operation
 */
public interface GlobalSplitInstallTask<TResult> {
    /**
     * Returns true if this task has completed, otherwise false
     *
     * @return whether or not the task has completed
     */
    boolean isComplete();

    /**
     * Returns true if this task is successful, otherwise false
     *
     * @return whether or not the task is successful
     */
    boolean isSuccessful();

    /**
     * Returns the result of this task, if the task is completed
     *
     * @return the result of the task
     */
    TResult getResult();

    /**
     * Returns the exception of the task, if it has failed
     *
     * @return the exception of the task
     */
    Exception getException();

    /**
     * Adds a listener to be notified of when this task succeeds
     *
     * @param onSuccessListener the listener to be notified
     * @return itself
     */
    GlobalSplitInstallTask<TResult> addOnSuccessListener(OnGlobalSplitInstallSuccessListener<TResult> onSuccessListener);

    /**
     * Adds a listener to be notified of when this task succeeds, the listener will be executed on
     * the provided {@link Executor}
     *
     * @param executor the {@link Executor} to execute the callback on
     * @param onSuccessListener the listener to be notified
     * @return itself
     */
    GlobalSplitInstallTask<TResult> addOnSuccessListener(Executor executor, OnGlobalSplitInstallSuccessListener<TResult> onSuccessListener);

    /**
     * Adds a listener to be notified of when this task fails
     *
     * @param onFailureListener the listener to be notified
     * @return itself
     */
    GlobalSplitInstallTask<TResult> addOnFailureListener(OnGlobalSplitInstallFailureListener onFailureListener);

    /**
     * Adds a listener to be notified of when this task fails, the listener will be executed on the
     * provided {@link Executor}
     *
     * @param executor the {@link Executor} to execute the callback on
     * @param onFailureListener the listener to be notified
     * @return itself
     */
    GlobalSplitInstallTask<TResult> addOnFailureListener(Executor executor, OnGlobalSplitInstallFailureListener onFailureListener);

    /**
     * Adds a listener to be notified of when this task completes
     *
     * @param onCompleteListener the listener to be notified
     * @return itself
     */
    GlobalSplitInstallTask<TResult> addOnCompleteListener(OnGlobalSplitInstallCompleteListener<TResult> onCompleteListener);

    /**
     * Adds a listener to be notified of when this task completes, the listener will be executed on
     * the provided {@link Executor}
     *
     * @param executor the {@link Executor} to execute the callback on
     * @param onCompleteListener the listener to be notified
     * @return itself
     */
    GlobalSplitInstallTask<TResult> addOnCompleteListener(Executor executor, OnGlobalSplitInstallCompleteListener<TResult> onCompleteListener);
}

class GlobalSplitInstallEmptyTask<TResult> implements GlobalSplitInstallTask<TResult> {

    private final TResult result;
    private final Exception exception;

    GlobalSplitInstallEmptyTask(TResult result) {
        this.result = result;
        this.exception = null;
    }

    GlobalSplitInstallEmptyTask(Exception exception) {
        this.result = null;
        this.exception = exception;
    }

    @Override
    public boolean isComplete() {
        return true;
    }

    @Override
    public boolean isSuccessful() {
        return exception == null;
    }

    @Override
    public TResult getResult() {
        return result;
    }

    @Override
    public Exception getException() {
        return exception;
    }

    @Override
    public GlobalSplitInstallTask<TResult> addOnSuccessListener(OnGlobalSplitInstallSuccessListener<TResult> onSuccessListener) {
        if (isSuccessful()) {
            onSuccessListener.onSuccess(result);
        }
        return this;
    }

    @Override
    public GlobalSplitInstallTask<TResult> addOnSuccessListener(java.util.concurrent.Executor executor, final OnGlobalSplitInstallSuccessListener<TResult> onSuccessListener) {
        if (isSuccessful()) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    onSuccessListener.onSuccess(result);
                }
            });
        }
        return this;
    }

    @Override
    public GlobalSplitInstallTask<TResult> addOnFailureListener(OnGlobalSplitInstallFailureListener onFailureListener) {
        if (!isSuccessful()) {
            onFailureListener.onFailure(exception);
        }
        return this;
    }

    @Override
    public GlobalSplitInstallTask<TResult> addOnFailureListener(java.util.concurrent.Executor executor, final OnGlobalSplitInstallFailureListener onFailureListener) {
        if (!isSuccessful()) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    onFailureListener.onFailure(exception);
                }
            });
        }
        return this;
    }

    @Override
    public GlobalSplitInstallTask<TResult> addOnCompleteListener(OnGlobalSplitInstallCompleteListener<TResult> onCompleteListener) {
        onCompleteListener.onComplete(this);
        return this;
    }

    @Override
    public GlobalSplitInstallTask<TResult> addOnCompleteListener(java.util.concurrent.Executor executor, final OnGlobalSplitInstallCompleteListener<TResult> onCompleteListener) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                onCompleteListener.onComplete(GlobalSplitInstallEmptyTask.this);
            }
        });
        return this;
    }
}
