package com.jeppeman.locallydynamic.integration_tests

import android.content.Context
import android.content.Intent
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import kotlin.reflect.KClass

fun <T : Feature<*>> KClass<T>.info(context: Context) = when {
    InstallTimeFeature::class.java.isAssignableFrom(java) -> Feature.Info(
        id = "installtimefeature",
        name = context.getString(R.string.title_installtimefeature),
        actionId = R.id.actionInstallTimeFeature
    )
    OnDemandFeature::class.java.isAssignableFrom(java) -> Feature.Info(
        id = "ondemandfeature",
        name = context.getString(R.string.title_ondemandfeature),
        actionId = R.id.actionOnDemandFeature
    )
    else -> throw IllegalArgumentException("Unexpected feature $this")
}

interface Feature<T> {
    fun getMainScreen(): Fragment
    fun getLaunchIntent(context: Context): Intent
    fun inject(dependencies: T)

    data class Info(
        val id: String,
        val name: String,
        @IdRes val actionId: Int
    )
}

interface InstallTimeFeature : Feature<InstallTimeFeature.Dependencies> {
    interface Dependencies {
        val context: Context
    }
}

interface OnDemandFeature : Feature<OnDemandFeature.Dependencies> {
    interface Dependencies {
        val context: Context
    }
}