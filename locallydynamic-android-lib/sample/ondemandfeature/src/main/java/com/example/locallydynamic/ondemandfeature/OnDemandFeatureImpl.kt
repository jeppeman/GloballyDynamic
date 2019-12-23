package com.example.locallydynamic.ondemandfeature

import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import com.example.locallydynamic.Feature
import com.example.locallydynamic.InstallTimeFeature
import com.example.locallydynamic.OnDemandFeature
import com.example.locallydynamic.info
import com.google.auto.service.AutoService

@AutoService(OnDemandFeature::class)
class OnDemandFeatureImpl : OnDemandFeature {
    override fun getMainScreen(): Fragment =
        OnDemandFragment()

    override fun getLaunchIntent(context: Context): Intent =
        Intent(context, OnDemandActivity::class.java)

    override fun inject(dependencies: OnDemandFeature.Dependencies) {
    }
}