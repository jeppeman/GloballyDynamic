package com.example.globallydynamic.ondemandfeature

import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import com.example.globallydynamic.Feature
import com.example.globallydynamic.InstallTimeFeature
import com.example.globallydynamic.OnDemandFeature
import com.example.globallydynamic.info
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