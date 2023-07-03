package com.jeppeman.globallydynamic.idea

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.jeppeman.globallydynamic.idea.tooling.globallyDynamicGradle
import com.jeppeman.globallydynamic.idea.utils.runInBackground
import com.jeppeman.globallydynamic.idea.utils.startGloballyDynamicServerIfNeeded

class GloballyDynamicStartupActivity : StartupActivity {
    override fun runActivity(project: Project) {
        project.runInBackground("Preparing GloballyDynamic") {
            project.globallyDynamicGradle.refresh()
            project.startGloballyDynamicServerIfNeeded()
        }
    }
}