package com.example.globallydynamic

import android.content.Context
import androidx.lifecycle.ViewModel
import com.example.globallydynamic.R
import kotlin.reflect.KClass

class GloballyDynamicViewModel(
        private val context: Context,
        private val featureManager: FeatureManager = FeatureManager(context),
        private val installTimeFeatureDeps: InstallTimeFeature.Dependencies,
        private val onDemandFeatureDeps: OnDemandFeature.Dependencies
) : ViewModel() {
    val featureInstalled = SingleLiveEvent<Feature.Info>()
    val languageInstalled = SingleLiveEvent<Language>()

    init {
        featureManager.registerInstallListener { featureInfo, language ->
            if (featureInfo != null) {
                featureInstalled.value = featureInfo
            }
            if (language != null) {
                languageInstalled.value = language
            }
        }
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

    fun isLanguageInstalled(languageType: KClass<out Language>): Boolean {
        return featureManager.isLanguageInstalled(languageType)
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