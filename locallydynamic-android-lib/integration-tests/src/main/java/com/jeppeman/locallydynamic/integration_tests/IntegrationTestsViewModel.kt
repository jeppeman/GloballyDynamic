package com.jeppeman.locallydynamic.integration_tests

import android.content.Context
import androidx.lifecycle.ViewModel

class IntegrationTestsViewModel(
    private val context: Context,
    private val featureManager: IntegrationTestsFeatureManager = IntegrationTestsFeatureManager(context),
    private val installTimeFeatureDeps: InstallTimeFeature.Dependencies,
    private val onDemandFeatureDeps: OnDemandFeature.Dependencies
) : ViewModel() {
    val featureInstalled = SingleLiveEvent<Feature.Info>()
    val installState = mutableLiveDataOf<IntegrationTestsFeatureManager.InstallState>()

    init {
        featureManager.registerInstallListener(featureInstalled::setValue)
    }


    fun installFeature(actionId: Int) {
        val listener: (IntegrationTestsFeatureManager.InstallState) -> Unit = { state ->
            installState.value = state
        }
        when (actionId) {
            InstallTimeFeature::class.info(context).actionId -> featureManager.installFeature<InstallTimeFeature>(listener)
            OnDemandFeature::class.info(context).actionId -> featureManager.installFeature<OnDemandFeature>(listener)
        }
    }

    fun installFeature(feature: String) {
        val listener: (IntegrationTestsFeatureManager.InstallState) -> Unit = { state ->
            installState.value = state
        }
        when (feature) {
            InstallTimeFeature::class.info(context).id -> featureManager.installFeature<InstallTimeFeature>(listener)
            OnDemandFeature::class.info(context).id -> featureManager.installFeature<OnDemandFeature>(listener)
        }
    }

    fun cancelInstall() {
        installState.value?.let { featureManager.cancelInstall(it.sessionId) }
    }

    fun isFeatureInstalled(actionId: Int): Boolean {
        return when (actionId) {
            R.id.actionInstallTimeFeature -> {
                featureManager.isFeatureInstalled<InstallTimeFeature>()
            }
            R.id.actionOnDemandFeature -> {
                featureManager.isFeatureInstalled<OnDemandFeature>()
            }
            else -> false
        }
    }

    fun getFeature(actionId: Int): Feature<*> {
        return when (actionId) {
            R.id.actionInstallTimeFeature -> {
                featureManager.getFeature<InstallTimeFeature, InstallTimeFeature.Dependencies>(
                    dependencies = installTimeFeatureDeps
                )
            }
            R.id.actionOnDemandFeature -> {
                featureManager.getFeature<OnDemandFeature, OnDemandFeature.Dependencies>(
                    dependencies = onDemandFeatureDeps
                )
            }
            else -> null
        } ?: throw IllegalArgumentException("Feature not found for action $actionId")
    }
}