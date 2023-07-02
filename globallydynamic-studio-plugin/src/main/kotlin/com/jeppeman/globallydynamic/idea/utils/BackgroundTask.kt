package com.jeppeman.globallydynamic.idea.utils

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import org.gradle.internal.impldep.com.google.common.util.concurrent.ListenableFuture
import org.gradle.internal.impldep.com.google.common.util.concurrent.SettableFuture


fun <T> Project.runInBackgroundAndWait(title: String, action: () -> T): T {
    val future = SettableFuture.create<T>()
    BackgroundTask(this, title, future, action).queue()
    return future.get()
}

fun <T> Project.runInBackground(title: String, action: () -> T): ListenableFuture<T> {
    val future = SettableFuture.create<T>()
    BackgroundTask(this, title, future, action).queue()
    return future

}

private class BackgroundTask<T>(
    project: Project,
    title: String,
    private val settableFuture: SettableFuture<T>,
    private val action: () -> T
) : Task.Backgroundable(project, title) {
    override fun run(progressIndicator: ProgressIndicator) {
        val result = action()
        settableFuture.set(result)
    }
}
