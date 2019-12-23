package com.jeppeman.locallydynamic.idea.extensions

import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

fun Project.showNotification(message: String, type: NotificationType) {
    val group = NotificationGroup("LOCALLY_DYNAMIC", NotificationDisplayType.BALLOON, true)
    group.createNotification(
        "Locally Dynamic",
        message,
        type,
        null
    ).notify(this)
}