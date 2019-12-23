package com.jeppeman.globallydynamic.globalsplitinstall;

import android.os.Handler;
import android.os.Looper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.MockitoAnnotations;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowLooper;

import java.util.concurrent.TimeUnit;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@RunWith(AndroidJUnit4.class)
public class ExecutorImplTest {
    private Handler backgroundHandler = spy(new Handler(Looper.getMainLooper()));
    private Handler foregroundHandler = spy(new Handler(Looper.getMainLooper()));

    private ExecutorImpl executorImpl = new ExecutorImpl(
            foregroundHandler,
            backgroundHandler
    );

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void whenTaskIsSuccessful_execute_shouldPostResultToForeground() throws Exception {
        final String[] result = new String[]{null};
        Executor.Callbacks<String> callbacks = spy(new Executor.Callbacks<String>() {
            @Override
            String execute() {
                return "hi";
            }

            @Override
            void onComplete(String r) {
                result[0] = r;
            }
        });

        InOrder inOrder = inOrder(backgroundHandler, foregroundHandler, callbacks);

        executorImpl.execute(callbacks);
        Shadow.<ShadowLooper>extract(backgroundHandler.getLooper()).runToEndOfTasks();

        inOrder.verify(backgroundHandler).postDelayed(any(Runnable.class), eq(0L));
        inOrder.verify(callbacks).execute();
        inOrder.verify(foregroundHandler).post(any(Runnable.class));
        inOrder.verify(callbacks).onComplete("hi");
        assertThat(result[0]).isEqualTo("hi");
    }

    @Test
    public void whenTaskFails_execute_shouldPostErrorToForeground() throws Exception {
        final IllegalStateException exception = new IllegalStateException();
        Executor.Callbacks<String> callbacks = spy(new Executor.Callbacks<String>() {
            @Override
            String execute() {
                throw exception;
            }
        });
        InOrder inOrder = inOrder(backgroundHandler, foregroundHandler, callbacks);

        executorImpl.execute(callbacks);
        Shadow.<ShadowLooper>extract(backgroundHandler.getLooper()).runToEndOfTasks();

        inOrder.verify(backgroundHandler).postDelayed(any(Runnable.class), eq(0L));
        inOrder.verify(callbacks).execute();
        inOrder.verify(foregroundHandler).post(any(Runnable.class));
        inOrder.verify(callbacks).onError(exception);
    }

    @Test
    public void schedule_shouldRunTaskAfterDelay() throws Exception {
        final IllegalStateException exception = new IllegalStateException();
        Executor.Callbacks<String> callbacks = spy(new Executor.Callbacks<String>() {
            @Override
            String execute() {
                throw exception;
            }
        });
        InOrder inOrder = inOrder(backgroundHandler, foregroundHandler, callbacks);
        long delay = 500L;

        executorImpl.schedule(delay, callbacks);
        Shadow.<ShadowLooper>extract(backgroundHandler.getLooper()).getScheduler().advanceBy(delay, TimeUnit.MILLISECONDS);

        inOrder.verify(backgroundHandler).postDelayed(any(Runnable.class), eq(delay));
        inOrder.verify(callbacks).execute();
        inOrder.verify(foregroundHandler).post(any(Runnable.class));
        inOrder.verify(callbacks).onError(exception);
    }

    @Test
    public void schedule_shouldNotRunTasksBeforeDelay() throws Exception {
        final IllegalStateException exception = new IllegalStateException();
        Executor.Callbacks<String> callbacks = spy(new Executor.Callbacks<String>() {
            @Override
            String execute() {
                throw exception;
            }
        });
        long delay = 500L;

        executorImpl.schedule(delay, callbacks);
        Shadow.<ShadowLooper>extract(backgroundHandler.getLooper()).getScheduler().advanceBy(delay - 300, TimeUnit.MILLISECONDS);

        verify(backgroundHandler).postDelayed(any(Runnable.class), eq(delay));
        verify(callbacks, never()).execute();
    }
}