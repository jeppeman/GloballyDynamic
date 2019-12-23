package com.jeppeman.globallydynamic.globalsplitinstall;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.atomic.AtomicInteger;

class ExecutorFactory {
    static Executor create() {
        return new ExecutorImpl();
    }
}

interface Executor {
    boolean isIdle();

    Handler getForegroundHandler();

    <T> void executeForeground(@NonNull Callbacks<T> callbacks);

    <T> void execute(@NonNull Callbacks<T> callbacks);

    <T> void schedule(long delay, @NonNull Callbacks<T> callbacks);

    abstract class Callbacks<T> {
        abstract T execute() throws Exception;

        void onComplete(T result) {

        }

        void onError(Exception exception) {

        }
    }
}

class ExecutorImpl implements Executor {
    private final Handler backgroundHandler;
    private final Handler foregroundHandler;
    private final AtomicInteger runningTasks = new AtomicInteger(0);

    ExecutorImpl(
            @Nullable Handler foregroundHandler,
            @Nullable Handler backgroundHandler) {
        if (backgroundHandler != null) {
            this.backgroundHandler = backgroundHandler;
        } else {
            HandlerThread handlerThread = new HandlerThread("globallydynamic-worker");
            handlerThread.start();
            this.backgroundHandler = new Handler(handlerThread.getLooper());
        }

        if (foregroundHandler != null) {
            this.foregroundHandler = foregroundHandler;
        } else {
            this.foregroundHandler = new Handler(Looper.getMainLooper());
        }
    }

    ExecutorImpl() {
        this(null, null);
    }

    @Override
    public boolean isIdle() {
        return runningTasks.get() == 0;
    }

    @Override
    public Handler getForegroundHandler() {
        return foregroundHandler;
    }

    private <T> void schedule(
            long delay,
            final @NonNull Callbacks<T> callbacks,
            final @NonNull Handler backgroundHandler,
            final @NonNull Handler foregroundHandler
    ) {
        runningTasks.incrementAndGet();
        backgroundHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    final T result = callbacks.execute();
                    foregroundHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callbacks.onComplete(result);
                            runningTasks.decrementAndGet();
                        }
                    });
                } catch (final Exception exception) {
                    foregroundHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callbacks.onError(exception);
                            runningTasks.decrementAndGet();
                        }
                    });
                }
            }
        }, delay);
    }

    @Override
    public <T> void executeForeground(@NonNull Callbacks<T> callbacks) {
        schedule(0, callbacks, foregroundHandler, foregroundHandler);
    }

    public <T> void execute(@NonNull Callbacks<T> callbacks) {
        schedule(0, callbacks, backgroundHandler, foregroundHandler);
    }


    @Override
    public <T> void schedule(long delay, @NonNull Callbacks<T> callbacks) {
        schedule(delay, callbacks, backgroundHandler, foregroundHandler);
    }
}