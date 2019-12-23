package com.jeppeman.locallydynamic.integration_tests

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.activity_integration_tests.*

private val TAG_TOP_FRAGMENT = "${IntegrationTestsActivity::class.java.name}.TOP_FRAGMENT"

class IntegrationTestsActivity : AppCompatActivity(),
    BottomNavigationView.OnNavigationItemSelectedListener {

    private lateinit var integrationTestsViewModel: IntegrationTestsViewModel

    private fun goToFeatureEntryPoint(@IdRes actionId: Int) {
        val transaction = supportFragmentManager.beginTransaction()

        val feature = integrationTestsViewModel.getFeature(actionId)
        val fragment = feature.getMainScreen()
        transaction.replace(R.id.fragmentContainer, fragment, TAG_TOP_FRAGMENT).commit()
    }

    private fun featureInstalled(featureInfo: Feature.Info) {
        bottomNavigation?.selectedItemId = featureInfo.actionId
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        Log.v("INSTALL??", "INSTALL??")
        integrationTestsViewModel.cancelInstall()
        val isInstalled = integrationTestsViewModel.isFeatureInstalled(item.itemId)

        return if (!isInstalled) {
            integrationTestsViewModel.installFeature(item.itemId)
            false
        } else {
            goToFeatureEntryPoint(item.itemId)
            true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_integration_tests)

        integrationTestsViewModel = ViewModelProviders.of(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return IntegrationTestsViewModel(
                    context = applicationContext,
                    installTimeFeatureDeps = object : InstallTimeFeature.Dependencies {
                        override val context: Context = applicationContext

                    },
                    onDemandFeatureDeps = object : OnDemandFeature.Dependencies {
                        override val context: Context = applicationContext
                    }
                ) as T
            }
        })[IntegrationTestsViewModel::class.java]

        if (savedInstanceState == null) {
            integrationTestsViewModel.installFeature(R.id.actionInstallTimeFeature)
        }

        integrationTestsViewModel.featureInstalled.observe(this, ::featureInstalled)

        bottomNavigation?.setOnNavigationItemSelectedListener(this)
    }
}