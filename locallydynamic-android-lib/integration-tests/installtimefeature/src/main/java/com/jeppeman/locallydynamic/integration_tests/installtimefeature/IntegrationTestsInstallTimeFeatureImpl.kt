package com.jeppeman.locallydynamic.integration_tests.installtimefeature

import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import com.google.auto.service.AutoService
import com.jeppeman.locallydynamic.integration_tests.InstallTimeFeature

@AutoService(InstallTimeFeature::class)
class IntegrationTestsInstallTimeFeatureImpl : InstallTimeFeature {
    override fun getMainScreen(): Fragment = IntegrationTestsInstallTimeFragment()

    override fun getLaunchIntent(context: Context): Intent =
        Intent(context, IntegrationTestsInstallTimeActivity::class.java)

    override fun inject(dependencies: InstallTimeFeature.Dependencies) {
    }
}