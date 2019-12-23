package com.example.locallydynamic.installtimefeature

import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import com.example.locallydynamic.InstallTimeFeature
import com.google.auto.service.AutoService

@AutoService(InstallTimeFeature::class)
class InstallTimeFeatureImpl : InstallTimeFeature {
    override fun getMainScreen(): Fragment = InstallTimeFragment()

    override fun getLaunchIntent(context: Context): Intent =
        Intent(context, InstallTimeActivity::class.java)

    override fun inject(dependencies: InstallTimeFeature.Dependencies) {
    }
}