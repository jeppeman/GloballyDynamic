package com.example.globallydynamic.minimal_sample

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.example.globallydynamic.minimal_sample.databinding.ActivityMainBinding
import com.jeppeman.globallydynamic.globalsplitinstall.*
import kotlin.math.roundToInt

private const val INSTALL_REQUEST_CODE = 123

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val globalSplitInstallManager: GlobalSplitInstallManager by lazy {
        GlobalSplitInstallManagerFactory.create(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.installButton.setOnClickListener {
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


        binding.inProgressGroup.visibility = View.VISIBLE
        binding.installButton.visibility = View.GONE

        var mySessionId = 0

        globalSplitInstallManager.registerListener { state ->
            if (state.sessionId() == mySessionId) {
                when (state.status()) {
                    GlobalSplitInstallSessionStatus.CANCELED -> {
                        binding.stateText.text = "Canceled"
                        binding.inProgressGroup.visibility = View.GONE
                        binding.installButton.visibility = View.VISIBLE
                    }

                    GlobalSplitInstallSessionStatus.DOWNLOADING -> {
                        binding.stateText.text = "Downloading feature"
                        binding.progressBar.progress =
                            (state.bytesDownloaded() * 100f / state.totalBytesToDownload()
                                .toFloat()).roundToInt()
                    }

                    GlobalSplitInstallSessionStatus.INSTALLING -> {
                        binding.progressBar.progress = 100
                        binding.stateText.text = "Installing feature"
                    }

                    GlobalSplitInstallSessionStatus.INSTALLED -> {
                        binding.stateText.text = "Successfully installed feature"
                        launchDynamicActivity()
                    }

                    GlobalSplitInstallSessionStatus.UNINSTALLED -> {
                        updateUi()
                    }

                    GlobalSplitInstallSessionStatus.UNINSTALLING -> {
                        binding.stateText.text = "Uninstalling feature"
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
            binding.stateText.text = "Install failed"
            binding.inProgressGroup.visibility = View.VISIBLE
            binding.installButton.visibility = View.VISIBLE
        }
    }

    private fun updateUi() {
        binding.stateText.text = ""
        binding.inProgressGroup.visibility = View.GONE
        binding.installButton.visibility = View.VISIBLE
        binding.installButton.text = if (globalSplitInstallManager.installedModules.isNotEmpty()) {
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
