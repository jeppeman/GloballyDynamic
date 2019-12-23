package com.jeppeman.locallydynamic.integration_tests

import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.IdlingResource
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus

class LocallyDynamicIdlingResource(
    private val splitInstallManager: SplitInstallManager =
        ApplicationProvider.getApplicationContext<IntegrationTestsApplication>().splitInstallManager
) : IdlingResource {
    private var wasIdle = false
    private var resourceCallback: IdlingResource.ResourceCallback? = null
    private val executor: Any? = splitInstallManager.javaClass.getDeclaredField("executor").apply {
        isAccessible = true
    }.get(splitInstallManager)

    private fun isExecutorIdle(): Boolean = executor!!.javaClass.getDeclaredMethod("isIdle").apply {
        isAccessible = true
    }.invoke(executor)!! as Boolean

    override fun getName(): String = "LocallyDynamicIdlingResource"

    override fun isIdleNow(): Boolean {
        val idleNow = !splitInstallManager.sessionStates.result.any {
            listOf(SplitInstallSessionStatus.DOWNLOADING,
                SplitInstallSessionStatus.DOWNLOADED,
                SplitInstallSessionStatus.PENDING,
                SplitInstallSessionStatus.INSTALLING).contains(it.status())
        } && isExecutorIdle()

        Log.v("idling", "${splitInstallManager.sessionStates.result.map { 
            it.status()
        }}")

        if (!wasIdle && resourceCallback != null) {
            resourceCallback?.onTransitionToIdle()
        }

        wasIdle = idleNow

        return idleNow
    }

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
        resourceCallback = callback
    }
}