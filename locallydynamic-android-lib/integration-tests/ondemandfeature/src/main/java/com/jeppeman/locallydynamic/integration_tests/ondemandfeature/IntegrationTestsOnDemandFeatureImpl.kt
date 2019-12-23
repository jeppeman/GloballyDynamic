package com.jeppeman.locallydynamic.integration_tests.ondemandfeature

import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import com.google.auto.service.AutoService
import com.jeppeman.locallydynamic.integration_tests.OnDemandFeature

@AutoService(OnDemandFeature::class)
class IntegrationTestsOnDemandFeatureImpl : OnDemandFeature {
    override fun getMainScreen(): Fragment = IntegrationTestsOnDemandFragment()

    override fun getLaunchIntent(context: Context): Intent =
        Intent(context, IntegrationTestsOnDemandActivity::class.java)

    override fun inject(dependencies: OnDemandFeature.Dependencies) {
    }
}