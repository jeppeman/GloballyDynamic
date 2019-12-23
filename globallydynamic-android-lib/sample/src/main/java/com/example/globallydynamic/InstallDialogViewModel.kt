package com.example.globallydynamic

import android.content.Context
import androidx.lifecycle.ViewModel

class InstallDialogViewModel(
    private val context: Context,
    private val featureManager: FeatureManager = FeatureManager(context)
) : ViewModel() {
    val installState = mutableLiveDataOf<FeatureManager.InstallState>()

    fun installFeature(actionId: Int) {
        val listener: (FeatureManager.InstallState) -> Unit = { state ->
            installState.value = state
        }
        when (actionId) {
            InstallTimeFeature::class.info(context).actionId -> featureManager.installFeature<InstallTimeFeature>(listener)
            OnDemandFeature::class.info(context).actionId -> featureManager.installFeature<OnDemandFeature>(listener)
        }
    }

    fun installLanguage(langCode: String) {
        val listener: (FeatureManager.InstallState) -> Unit = { state ->
            installState.value = state
        }
        when (langCode) {
            "sv" -> featureManager.installLanguage(Swedish::class, listener)
            "ko" -> featureManager.installLanguage(Korean::class, listener)
            "de" -> featureManager.installLanguage(German::class, listener)
            "it" -> featureManager.installLanguage(Italian::class, listener)
        }
    }

    fun cancelInstall() {
        featureManager.cancelInstall(installState.value!!.sessionId)
    }
}