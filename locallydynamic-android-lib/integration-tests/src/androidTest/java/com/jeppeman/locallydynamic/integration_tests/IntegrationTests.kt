package com.jeppeman.locallydynamic.integration_tests

import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class IntegrationTests {
    private val idlingResource = LocallyDynamicIdlingResource()
    private val uiDevice: UiDevice by lazy {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }
    private val app: IntegrationTestsApplication by lazy {
        ApplicationProvider.getApplicationContext<IntegrationTestsApplication>()
    }
    private val appPackageName: String by lazy {
        app.packageName
    }

    private val toggleButtonClasses = listOf(
        "android.widget.Switch",
        "androidx.appcompat.widget.SwitchCompat",
        "android.widget.ToggleButton",
        "android.widget.CheckBox"
    )

    private fun UiDevice.findObjectsAndPrint(bySelector: BySelector): List<UiObject2> {
        return (findObjects(bySelector) ?: listOf()).apply {
            app.log("$bySelector --- ${map { "${it.className} ${it.text}" }}")
        }
    }

    private fun UiDevice.goThroughInstallConfirmationFlow() {
        // Wait for app to go to background
        wait(Until.gone(By.pkg(appPackageName)), 60000L)

        val settingsSelector = By.text(
            Pattern.compile(
                "(?i:(.*settings.*))",
                Pattern.CASE_INSENSITIVE
            )
        )

        val installSelector = By.text(
            Pattern.compile(
                "(?i:(.*install.*))",
                Pattern.CASE_INSENSITIVE
            )
        )

        if (hasObject(settingsSelector)) {
            findObjectsAndPrint(settingsSelector).tryClick()

            // Wait for allow toggle to appear
            val allowSelector = By.text(
                Pattern.compile(
                    "(?i:(.*allow.*))",
                    Pattern.CASE_INSENSITIVE
                )
            )

            wait(Until.hasObject(allowSelector), UI_DEVICE_TIMEOUT)

            if (!hasObject(allowSelector)) {
                // Scroll down to the allow setting
                val selector = UiSelector().textMatches("(?i:(.*allow.*))")
                UiScrollable(UiSelector().scrollable(true))
                    .scrollIntoView(selector)

                findObject(selector).click()

                // Give it some time to settle
                Thread.sleep(2000)
            }

            // Try to find the allow switch and toggle it
            toggleButtonClasses.flatMap { clazz ->
                findObjectsAndPrint(By.clazz(clazz))
            }.tryClick()

            // Give it some time to settle
            Thread.sleep(2000)

            // On some devices an extra confirm dialog appears
            val okSelector = By.text(
                Pattern.compile(
                    "(?i:(ok))",
                    Pattern.CASE_INSENSITIVE
                )
            )

            if (hasObject(okSelector) || hasObject(allowSelector.clazz("android.widget.Button"))) {
                val okCandidates = findObjectsAndPrint(okSelector)

                if (okCandidates.isNotEmpty()) {
                    okCandidates.tryClick()
                } else {
                    findObjectsAndPrint(allowSelector).tryClick()
                }

                wait(Until.gone(okSelector), UI_DEVICE_TIMEOUT)

                pressBack()

                wait(Until.hasObject(By.pkg(appPackageName)), UI_DEVICE_TIMEOUT)

                findObjectsAndPrint(installSelector).tryClick()

                goThroughInstallConfirmationFlow()

                return
            }

            pressBack()
        }

        // Do install
        wait(Until.hasObject(installSelector), UI_DEVICE_TIMEOUT)

        findObjectsAndPrint(installSelector)
            .filter { it.className == "android.widget.Button" }
            .tryClick()

        wait(Until.gone(installSelector), UI_DEVICE_TIMEOUT)

        // An extra confirm dialog for some devices
        val acceptAllowSelector = By.text(
            Pattern.compile(
                "(?i:(.*accept.*)|(.*allow*.))",
                Pattern.CASE_INSENSITIVE
            )
        )

        // Give the dialog a chance to appear
        Thread.sleep(5000)

        if (hasObject(acceptAllowSelector)) {
            findObjectsAndPrint(acceptAllowSelector).tryClick()
        }

        val sendSelector = By.text(
            Pattern.compile(
                "(?i:(.*don't send.*))",
                Pattern.CASE_INSENSITIVE
            )
        )

        if (hasObject(sendSelector)) {
            findObjectsAndPrint(sendSelector).tryClick()
        }

        // Give it some time to settle
        Thread.sleep(5000)
    }

    @Before
    fun setUp() {
        IdlingPolicies.setIdlingResourceTimeout(60, TimeUnit.SECONDS)
        IdlingRegistry.getInstance().register(idlingResource)
    }

    @After
    fun tearDown() {
        IdlingRegistry.getInstance().unregister(idlingResource)
    }

    @Test
    fun onDemandInstallationFlow() {
        ActivityScenario.launch(IntegrationTestsActivity::class.java)
        // Install install time feature
        uiDevice.goThroughInstallConfirmationFlow()
        onView(withText("Hi, I am a fragment from an install time feature")).check(
            matches(
                isDisplayed()
            )
        )
        // Install on demand feature
        IdlingRegistry.getInstance().unregister(idlingResource)
        onView(withId(R.id.actionOnDemandFeature)).check(matches(isDisplayed())).perform(click())
        IdlingRegistry.getInstance().register(idlingResource)
        uiDevice.goThroughInstallConfirmationFlow()
        onView(withText("Hi, I am a fragment from an on demand feature")).check(
            matches(
                isDisplayed()
            )
        )
    }

    companion object {
        private const val UI_DEVICE_TIMEOUT = 15000L
    }
}

private fun List<UiObject2>.tryClick() = forEach(UiObject2::tryClick)

private fun UiObject2.tryClick() {
    try {
        click()
    } catch (exception: Exception) {
        // Most likely StaleObjectException, ignoring..
    }
}
