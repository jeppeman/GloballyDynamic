package com.example.locallydynamic

import android.content.Context
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
import com.google.android.play.core.splitcompat.SplitCompat
import kotlinx.android.synthetic.main.activity_locally_dynamic.*
import kotlin.reflect.KClass


private val TAG_TOP_FRAGMENT = "${LocallyDynamicActivity::class.java.name}.TOP_FRAGMENT"

class LocallyDynamicActivity : AppCompatActivity(),
    BottomNavigationView.OnNavigationItemSelectedListener {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var locallyDynamicViewModel: LocallyDynamicViewModel

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
        val feature = locallyDynamicViewModel.getFeature(actionId)
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
        conf.locale = Language.create(this@LocallyDynamicActivity, this).locale
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        val resources = Resources(assets, metrics, conf)

        return resources.getString(id)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (item.itemId == bottomNavigation?.selectedItemId) {
            return false
        }

        val isInstalled = locallyDynamicViewModel.isFeatureInstalled(item.itemId)

        return if (!isInstalled) {
            launchInstallDialog(item.itemId)
            false
        } else {
            goToFeatureEntryPoint(item.itemId)
            true
        }
    }

    private fun initiateLanguageContainers() {
        SplitCompat.installActivity(this)
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

        if (locallyDynamicViewModel.isLanguageInstalled(Swedish::class)) {
            coverSv?.visibility = View.GONE
            downloadSv?.visibility = View.GONE
            helloSv?.text = Swedish::class.getString(R.string.hello)
        } else {
            coverSv?.visibility = View.VISIBLE
            downloadSv?.visibility = View.VISIBLE
            helloSv?.text = getString(R.string.swedish)
        }

        if (locallyDynamicViewModel.isLanguageInstalled(Korean::class)) {
            coverKo?.visibility = View.GONE
            downloadKo?.visibility = View.GONE
            helloKo?.text = Korean::class.getString(R.string.hello)
        } else {
            coverKo?.visibility = View.VISIBLE
            downloadKo?.visibility = View.VISIBLE
            helloKo?.text = getString(R.string.korean)
        }

        if (locallyDynamicViewModel.isLanguageInstalled(German::class)) {
            coverDe?.visibility = View.GONE
            downloadDe?.visibility = View.GONE
            helloDe?.text = German::class.getString(R.string.hello)
        } else {
            coverDe?.visibility = View.VISIBLE
            downloadDe?.visibility = View.VISIBLE
            helloDe?.text = getString(R.string.german)
        }

        if (locallyDynamicViewModel.isLanguageInstalled(Italian::class)) {
            coverIt?.visibility = View.GONE
            downloadIt?.visibility = View.GONE
            helloIt?.text = Italian::class.getString(R.string.hello)
        } else {
            coverIt?.visibility = View.VISIBLE
            downloadIt?.visibility = View.VISIBLE
            helloIt?.text = getString(R.string.italian)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_locally_dynamic)

        locallyDynamicViewModel = ViewModelProviders.of(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return LocallyDynamicViewModel(
                    context = applicationContext,
                    installTimeFeatureDeps = object : InstallTimeFeature.Dependencies {
                        override val context: Context = applicationContext

                    },
                    onDemandFeatureDeps = object : OnDemandFeature.Dependencies {
                        override val context: Context = applicationContext
                    }
                ) as T
            }
        })[LocallyDynamicViewModel::class.java]

        if (savedInstanceState == null) {
            if (locallyDynamicViewModel.isFeatureInstalled(R.id.actionInstallTimeFeature)) {
                goToFeatureEntryPoint(R.id.actionInstallTimeFeature)
            } else {
                launchInstallDialog(R.id.actionInstallTimeFeature)
            }
        }

        initiateLanguageContainers()

        locallyDynamicViewModel.featureInstalled.observe(this, ::featureInstalled)
        locallyDynamicViewModel.languageInstalled.observe(this, ::languageInstalled)

        bottomNavigation?.setOnNavigationItemSelectedListener(this)
    }
}