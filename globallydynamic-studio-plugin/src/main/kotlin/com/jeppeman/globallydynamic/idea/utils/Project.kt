package com.jeppeman.globallydynamic.idea.utils

import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.jeppeman.globallydynamic.idea.globallyDynamicServerManager
import com.jeppeman.globallydynamic.idea.tooling.hasGloballyDynamicEnabled

fun Project.showNotification(message: String, type: NotificationType) {
    val group = NotificationGroup("GLOBALLY_DYNAMIC", NotificationDisplayType.BALLOON, true)
    group.createNotification(
        "GloballyDynamic",
        message,
        type,
        null
    ).notify(this)
}

fun Project.startGloballyDynamicServerIfNeeded() {
    if (!hasGloballyDynamicEnabled) return

    globallyDynamicServerManager.start()
}