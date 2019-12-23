package com.example.globallydynamic

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.example.globallydynamic.R
import kotlinx.android.synthetic.main.install_feature_dialog.*

private const val ARG_LANGUAGE_CODE = "com.example.globallydynamic.ARG_LANGUAGE_CODE"
private const val ARG_FEATURE_ACTION_ID = "com.example.globallydynamic.ARG_FEATURE_ACTION_ID"

const val INSTALL_REQUEST_CODE = 123

fun createInstallDialogFragment(feature: String): InstallDialogFragment =
    InstallDialogFragment().apply {
        arguments = Bundle().apply {
            putString(ARG_LANGUAGE_CODE, feature)
        }
    }

fun createInstallDialogFragment(feature: Int): InstallDialogFragment =
    InstallDialogFragment().apply {
        arguments = Bundle().apply {
            putInt(ARG_FEATURE_ACTION_ID, feature)
        }
    }

class InstallDialogFragment : DialogFragment() {
    lateinit var installDialogViewModel: InstallDialogViewModel

    private fun progressTo(to: Int) {
        if (to > loader?.progress ?: 0) {
            progressValueText?.animateProgress(loader?.progress ?: 0, to, 333)
            loader?.animateBetween(loader?.progress ?: 0, to, 333)
        }
    }

    private fun handleDownloadingState(state: FeatureManager.InstallState.Downloading) {
        progressText?.text = getString(R.string.install_dialog_step_downloading)
        progressTo(state.progress)
    }

    private fun handleInstallingState(state: FeatureManager.InstallState.Installing) {
        installDialogContainer?.transitionToState(R.id.installing)
        progressText?.text = getString(R.string.install_dialog_installing)
        progressTo(state.progress)
    }

    private fun handleInstalledState(state: FeatureManager.InstallState.Installed) {
        progressText?.text = getString(R.string.install_dialog_installed)
        progressTo(100)
        throttleDismiss()
    }

    private fun handleFailedState(state: FeatureManager.InstallState.Failed) {
        progressText?.text = getString(R.string.install_dialog_failed, state.code.toString())

    }

    private fun handleUserConfirmationRequired(state: FeatureManager.InstallState.RequiresUserConfirmation) {
        activity?.let { state.startConfirmationDialogForResult(it, INSTALL_REQUEST_CODE) }
    }

    private fun handleCanceledState(state: FeatureManager.InstallState.Canceled) {
        progressText?.text = getString(R.string.install_dialog_canceled)
        throttleDismiss()
    }

    private fun onInstallStateChanged(state: FeatureManager.InstallState?) {
        if (state != null) {
            dialog?.setTitle(if (state.featureInfo != null) {
                getString(R.string.install_dialog_title_feature, state.featureInfo.name)
            } else {
                getString(R.string.install_dialog_title_language, state.language?.displayName)
            })
            installDialogContainer?.transitionToEnd()
        }

        when (state) {
            is FeatureManager.InstallState.RequiresUserConfirmation -> handleUserConfirmationRequired(
                state
            )
            is FeatureManager.InstallState.Downloading -> handleDownloadingState(state)
            is FeatureManager.InstallState.Installing -> handleInstallingState(state)
            is FeatureManager.InstallState.Installed -> handleInstalledState(state)
            is FeatureManager.InstallState.Failed -> handleFailedState(state)
            is FeatureManager.InstallState.Canceled -> handleCanceledState(state)
        }
    }

    private fun throttleDismiss() {
        view?.postDelayed({
            if (fragmentManager != null) {
                dismiss()
            }
        }, 500)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        installDialogViewModel = ViewModelProviders.of(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return InstallDialogViewModel(
                        context = requireContext()
                ) as T
            }

        })[InstallDialogViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.DialogTheme)

        if (savedInstanceState == null) {
            val languageCode = arguments?.getString(ARG_LANGUAGE_CODE)
            val featureActionId = arguments?.getInt(ARG_FEATURE_ACTION_ID)
            when {
                languageCode != null -> installDialogViewModel.installLanguage(languageCode)
                featureActionId != null -> installDialogViewModel.installFeature(featureActionId)
                else -> dismiss()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.install_feature_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        installDialogViewModel.installState.observe(this, ::onInstallStateChanged)

        dialog?.window?.setWindowAnimations(R.style.DialogAnimation)
        dialog?.setCanceledOnTouchOutside(false)
        cancelInstall?.setOnClickListener { installDialogViewModel.cancelInstall() }
    }
}