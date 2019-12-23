package com.jeppeman.locallydynamic.integration_tests

import android.util.Log
import com.google.android.play.core.splitcompat.SplitCompatApplication
import com.google.android.play.core.splitinstall.SplitInstallManager

class IntegrationTestsApplication : SplitCompatApplication() {
    val splitInstallManager: SplitInstallManager by lazy {
        com.jeppeman.locallydynamic.LocallyDynamicSplitInstallManagerFactory.create(this)
    }

    fun log(message: String) {
        Log.v("loggylogg", message)
    }
}
