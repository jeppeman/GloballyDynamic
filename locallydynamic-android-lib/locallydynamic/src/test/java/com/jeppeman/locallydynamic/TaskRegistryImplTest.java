package com.jeppeman.locallydynamic;

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;

public class TaskRegistryImplTest {
    private TaskRegistryImpl taskRegistry = new TaskRegistryImpl();

    @Test
    public void registerTask_shouldRegister() {
        LocallyDynamicInstallTask task = mock(LocallyDynamicInstallTask.class);

        taskRegistry.registerTask(task);

        assertThat(taskRegistry.getTasks()).hasSize(1);
        assertThat(taskRegistry.getTasks().get(0)).isSameAs(task);
    }

    @Test
    public void unregisterTask_shouldUnregister() {
        LocallyDynamicInstallTask task = mock(LocallyDynamicInstallTask.class);
        taskRegistry.registerTask(task);
        assertThat(taskRegistry.getTasks()).hasSize(1);
        assertThat(taskRegistry.getTasks().get(0)).isSameAs(task);

        taskRegistry.unregisterTask(task);

        assertThat(taskRegistry.getTasks()).isEmpty();
    }
}