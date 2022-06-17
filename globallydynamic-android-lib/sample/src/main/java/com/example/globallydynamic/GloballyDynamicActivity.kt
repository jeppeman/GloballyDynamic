package com.example.globallydynamic

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.MenuItem
import android.view.View
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.jeppeman.globallydynamic.globalsplitcompat.GlobalSplitCompat
import com.jeppeman.globallydynamic.globalsplitinstall.GlobalSplitInstallConfirmResult
import kotlinx.android.synthetic.main.activity_globally_dynamic.*
import kotlin.reflect.KClass


private val TAG_TOP_FRAGMENT = "${GloballyDynamicActivity::class.java.name}.TOP_FRAGMENT"

class GloballyDynamicActivity : AppCompatActivity(),
    BottomNavigationView.OnNavigationItemSelectedListener {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var globallyDynamicViewModel: GloballyDynamicViewModel

    private fun goToFeatureEntryPoint(@IdRes actionId: Int) {
        val transaction = supportFragmentManager.beginTransaction()
        when (actionId) {
            R.id.actionInstallTimeFeature -> transaction.setCustomAnimations(
                    R.anim.slide_out_right,
                    R.anim.slide_in_right
            )
            R.id.actionOnDemandFeature -> transaction.setCustomAnimations(
                    R.anim.slide_in_left,
                    R.anim.slide_out_left
            )
        }

        // Instead of replacing we hide the current, add the next and then remove the previous
        // in order to have child fragments be part of the animation
        val topFragment = supportFragmentManager.findFragmentByTag(TAG_TOP_FRAGMENT)
        val feature = globallyDynamicViewModel.getFeature(actionId)
        val fragment = feature.getMainScreen()
        transaction.add(R.id.fragmentContainer, fragment, TAG_TOP_FRAGMENT)
        if (topFragment != null) {
            transaction.hide(topFragment).commit()
            handler.postDelayed({
                supportFragmentManager.beginTransaction().remove(topFragment).commit()
            }, resources.getInteger(android.R.integer.config_mediumAnimTime).toLong())
        } else {
            transaction.commit()
        }
    }

    private fun launchInstallDialog(@IdRes actionId: Int) {
        createInstallDialogFragment(actionId).show(supportFragmentManager, "install")
    }

    private fun launchInstallDialog(langCode: String) {
        createInstallDialogFragment(langCode).show(supportFragmentManager, "install")
    }

    private fun featureInstalled(featureInfo: Feature.Info) {
        bottomNavigation?.selectedItemId = featureInfo.actionId
        initiateLanguageContainers()
    }

    private fun languageInstalled(language: Language) {
        initiateLanguageContainers()
    }

    private fun KClass<out Language>.getString(@StringRes id: Int): String {
        val conf = resources.configuration
        conf.locale = Language.create(this@GloballyDynamicActivity, this).locale
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        val resources = Resources(assets, metrics, conf)

        return resources.getString(id)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (item.itemId == bottomNavigation?.selectedItemId) {
            return false
        }

        val isInstalled = globallyDynamicViewModel.isFeatureInstalled(item.itemId)

        return if (!isInstalled) {
            launchInstallDialog(item.itemId)
            false
        } else {
            goToFeatureEntryPoint(item.itemId)
            true
        }
    }

    private fun initiateLanguageContainers() {
        GlobalSplitCompat.installActivity(this)
        listOf(
            swedishContainer,
            koreanContainer,
            germanContainer,
            italianContainer
        ).forEach { container ->
            container?.setOnClickListener {
                launchInstallDialog(it.tag.toString())
            }
        }

        if (globallyDynamicViewModel.isLanguageInstalled(Swedish::class)) {
            coverSv?.visibility = View.GONE
            downloadSv?.visibility = View.GONE
            helloSv?.text = Swedish::class.getString(R.string.hello)
        } else {
            coverSv?.visibility = View.VISIBLE
            downloadSv?.visibility = View.VISIBLE
            helloSv?.text = getString(R.string.swedish)
        }

        if (globallyDynamicViewModel.isLanguageInstalled(Korean::class)) {
            coverKo?.visibility = View.GONE
            downloadKo?.visibility = View.GONE
            helloKo?.text = Korean::class.getString(R.string.hello)
        } else {
            coverKo?.visibility = View.VISIBLE
            downloadKo?.visibility = View.VISIBLE
            helloKo?.text = getString(R.string.korean)
        }

        if (globallyDynamicViewModel.isLanguageInstalled(German::class)) {
            coverDe?.visibility = View.GONE
            downloadDe?.visibility = View.GONE
            helloDe?.text = German::class.getString(R.string.hello)
        } else {
            coverDe?.visibility = View.VISIBLE
            downloadDe?.visibility = View.VISIBLE
            helloDe?.text = getString(R.string.german)
        }

        if (globallyDynamicViewModel.isLanguageInstalled(Italian::class)) {
            coverIt?.visibility = View.GONE
            downloadIt?.visibility = View.GONE
            helloIt?.text = Italian::class.getString(R.string.hello)
        } else {
            coverIt?.visibility = View.VISIBLE
            downloadIt?.visibility = View.VISIBLE
            helloIt?.text = getString(R.string.italian)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == INSTALL_REQUEST_CODE
                &&  data?.hasExtra(GlobalSplitInstallConfirmResult.EXTRA_RESULT) == true) {
            supportFragmentManager.findFragmentByTag("install")?.let {
                supportFragmentManager.beginTransaction()
                        .remove(it)
                        .commit()
            }
            val installConfirmResult = data.getIntExtra(
                    GlobalSplitInstallConfirmResult.EXTRA_RESULT,
                    GlobalSplitInstallConfirmResult.RESULT_DENIED
            )
            if (installConfirmResult == GlobalSplitInstallConfirmResult.RESULT_CONFIRMED) {
                launchInstallDialog(R.id.actionOnDemandFeature)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_globally_dynamic)

        globallyDynamicViewModel = ViewModelProviders.of(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return GloballyDynamicViewModel(
                        context = applicationContext,
                        installTimeFeatureDeps = object : InstallTimeFeature.Dependencies {
                            override val context: Context = applicationContext

                        },
                        onDemandFeatureDeps = object : OnDemandFeature.Dependencies {
                            override val context: Context = applicationContext
                        }
                ) as T
            }
        })[GloballyDynamicViewModel::class.java]

        if (savedInstanceState == null) {
            if (globallyDynamicViewModel.isFeatureInstalled(R.id.actionInstallTimeFeature)) {
                goToFeatureEntryPoint(R.id.actionInstallTimeFeature)
            } else {
                launchInstallDialog(R.id.actionInstallTimeFeature)
            }
        }

        initiateLanguageContainers()

        globallyDynamicViewModel.featureInstalled.observe(this, ::featureInstalled)
        globallyDynamicViewModel.languageInstalled.observe(this, ::languageInstalled)

        bottomNavigation?.setOnNavigationItemSelectedListener(this)
    }
}