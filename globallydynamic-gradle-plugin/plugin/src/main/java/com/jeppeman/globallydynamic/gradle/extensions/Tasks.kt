package com.jeppeman.globallydynamic.gradle.extensions

import com.jeppeman.globallydynamic.gradle.VariantTaskAction
import org.gradle.api.Task
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider

internal inline fun <reified T : Task> TaskContainer.register(variantTaskAction: VariantTaskAction<T>): TaskProvider<T> =
    register(variantTaskAction.name, T::class.java) { task -> variantTaskAction.execute(task) }