package com.jeppeman.globallydynamic.globalsplitinstall;

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;

public class TaskRegistryImplTest {
    private TaskRegistryImpl taskRegistry = new TaskRegistryImpl();

    @Test
    public void registerTask_shouldRegister() {
        InstallTask task = mock(InstallTask.class);

        taskRegistry.registerTask(task);

        assertThat(taskRegistry.getTasks()).hasSize(1);
        assertThat(taskRegistry.getTasks().get(0)).isSameAs(task);
    }

    @Test
    public void unregisterTask_shouldUnregister() {
        InstallTask task = mock(InstallTask.class);
        taskRegistry.registerTask(task);
        assertThat(taskRegistry.getTasks()).hasSize(1);
        assertThat(taskRegistry.getTasks().get(0)).isSameAs(task);

        taskRegistry.unregisterTask(task);

        assertThat(taskRegistry.getTasks()).isEmpty();
    }
}