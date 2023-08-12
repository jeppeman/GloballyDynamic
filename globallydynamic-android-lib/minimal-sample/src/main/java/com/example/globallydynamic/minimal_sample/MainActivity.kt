package com.example.globallydynamic.minimal_sample

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.jeppeman.globallydynamic.globalsplitinstall.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.roundToInt

private const val INSTALL_REQUEST_CODE = 123

class MainActivity : AppCompatActivity() {

    private val globalSplitInstallManager: GlobalSplitInstallManager by lazy {
        GlobalSplitInstallManagerFactory.create(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        installButton.setOnClickListener {
            installDynamicFeature(globalSplitInstallManager.installedModules.isNotEmpty())
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == INSTALL_REQUEST_CODE
            && data?.hasExtra(GlobalSplitInstallConfirmResult.EXTRA_RESULT) == true
        ) {
            val confirmResult = data.getIntExtra(
                GlobalSplitInstallConfirmResult.EXTRA_RESULT,
                GlobalSplitInstallConfirmResult.RESULT_DENIED
            )

            if (confirmResult == GlobalSplitInstallConfirmResult.RESULT_CONFIRMED) {
                // User granted permission, install again!
                installDynamicFeature(globalSplitInstallManager.installedModules.isNotEmpty())
            }
        }
    }

    private fun launchDynamicActivity() {
        val dynamicActivityClass =
            Class.forName("com.example.globallydynamic.minimal_sample.dynamicfeature.DynamicActivity")
        startActivity(Intent(this, dynamicActivityClass))

    }

    private fun installDynamicFeature(isUninstall: Boolean) {
        val request = GlobalSplitInstallRequest.newBuilder()
            .addModule("dynamicfeature")
            .build()


        inProgressGroup.visibility = View.VISIBLE
        installButton.visibility = View.GONE

        var mySessionId = 0

        globalSplitInstallManager.registerListener { state ->
            if (state.sessionId() == mySessionId) {
                when (state.status()) {
                    GlobalSplitInstallSessionStatus.CANCELED -> {
                        stateText.text = "Canceled"
                        inProgressGroup.visibility = View.GONE
                        installButton.visibility = View.VISIBLE
                    }

                    GlobalSplitInstallSessionStatus.DOWNLOADING -> {
                        stateText.text = "Downloading feature"
                        progressBar.progress =
                            (state.bytesDownloaded() * 100f / state.totalBytesToDownload()
                                .toFloat()).roundToInt()
                    }

                    GlobalSplitInstallSessionStatus.INSTALLING -> {
                        progressBar.progress = 100
                        stateText.text = "Installing feature"
                    }

                    GlobalSplitInstallSessionStatus.INSTALLED -> {
                        stateText.text = "Successfully installed feature"
                        launchDynamicActivity()
                    }

                    GlobalSplitInstallSessionStatus.UNINSTALLED -> {
                        updateUi()
                    }

                    GlobalSplitInstallSessionStatus.UNINSTALLING -> {
                        stateText.text = "Uninstalling feature"
                    }

                    GlobalSplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION -> {
                        globalSplitInstallManager.startConfirmationDialogForResult(
                            state,
                            this,
                            INSTALL_REQUEST_CODE
                        )
                    }
                }
            }
        }
        if (isUninstall) {
            globalSplitInstallManager.startUninstall(listOf("dynamicfeature"))
        } else {
            globalSplitInstallManager.startInstall(request)
        }.addOnSuccessListener { sessionId ->
            if (sessionId == 0) {
                // Already installed
                launchDynamicActivity()
            } else {
                mySessionId = sessionId
            }
        }.addOnFailureListener {
            stateText.text = "Install failed"
            inProgressGroup.visibility = View.VISIBLE
            installButton.visibility = View.VISIBLE
        }
    }

    private fun updateUi() {
        stateText.text = ""
        inProgressGroup.visibility = View.GONE
        installButton.visibility = View.VISIBLE
        installButton.text = if (globalSplitInstallManager.installedModules.isNotEmpty()) {
            "Uninstall feature"
        } else {
            "Install feature"
        }
    }

    override fun onResume() {
        super.onResume()
        updateUi();
    }
}
