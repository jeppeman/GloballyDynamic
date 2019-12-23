package com.example.locallydynamic

import android.content.Context
import android.content.IntentSender
import android.os.Build
import androidx.annotation.RequiresApi
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.google.android.play.core.splitinstall.SplitInstallSessionState
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import java.util.*
import kotlin.math.roundToInt
import kotlin.reflect.KClass

inline fun <reified T : Feature<D>, D> FeatureManager.getFeature(
    dependencies: D
): T? {
    return if (isFeatureInstalled<T>()) {
        val serviceIterator = ServiceLoader.load(
            T::class.java,
            T::class.java.classLoader
        ).iterator()

        if (serviceIterator.hasNext()) {
            val feature = serviceIterator.next()
            feature.apply { inject(dependencies) }
        } else {
            null
        }
    } else {
        null
    }
}

inline fun <reified T : Feature<*>> FeatureManager.installFeature(
    noinline onStateUpdate: (FeatureManager.InstallState) -> Unit
) = installFeature(T::class, onStateUpdate)

inline fun <reified T : Feature<*>> FeatureManager.isFeatureInstalled(): Boolean =
    isFeatureInstalled(T::class)

inline fun <reified T : Language> FeatureManager.isLanguageInstalled(): Boolean =
    isLanguageInstalled(T::class)

interface FeatureManager {
    fun <T : Feature<*>> installFeature(
        featureType: KClass<T>,
        onStateUpdate: (InstallState) -> Unit
    )

    fun <T : Language> installLanguage(
        languageType: KClass<T>,
        onStateUpdate: (InstallState) -> Unit
    )

    fun isLanguageInstalled(languageType: KClass<out Language>): Boolean
    fun <T : Feature<*>> isFeatureInstalled(featureType: KClass<T>): Boolean
    fun cancelInstall(sessionId: Int)
    fun registerInstallListener(listener: (Feature.Info?, Language?) -> Unit)
    fun unregisterInstallListener(listener: (Feature.Info?, Language?) -> Unit)

    sealed class InstallState(
        val featureInfo: Feature.Info?,
        val language: Language?,
        val sessionId: Int
    ) {
        class Downloading(
            val progress: Int,
            featureInfo: Feature.Info? = null,
            language: Language? = null,
            sessionId: Int
        ) : InstallState(featureInfo, language, sessionId)

        class Installing(
            val progress: Int,
            featureInfo: Feature.Info? = null,
            language: Language? = null,
            sessionId: Int
        ) : InstallState(featureInfo, language, sessionId)

        class RequiresUserConfirmation(
            val sender: IntentSender?,
            featureInfo: Feature.Info? = null,
            language: Language? = null,
            sessionId: Int
        ) : InstallState(featureInfo, language, sessionId)

        class Failed(
            val code: Int,
            featureInfo: Feature.Info? = null,
            language: Language? = null,
            sessionId: Int
        ) : InstallState(featureInfo, language, sessionId)

        class Installed(
            featureInfo: Feature.Info? = null,
            language: Language? = null,
            sessionId: Int
        ) : InstallState(featureInfo, language, sessionId)

        class Canceled(
            featureInfo: Feature.Info? = null,
            language: Language? = null,
            sessionId: Int
        ) : InstallState(featureInfo, language, sessionId)
    }

    companion object : (Context) -> FeatureManager {
        private lateinit var instance: FeatureManager
        override fun invoke(context: Context): FeatureManager = if (::instance.isInitialized) {
            instance
        } else {
            FeatureManagerImpl(context).apply {
                instance = this
            }
        }
    }
}

internal class FeatureManagerImpl(
    private val context: Context
) : FeatureManager {
    private val splitInstallManager: SplitInstallManager =
        com.jeppeman.locallydynamic.LocallyDynamicSplitInstallManagerFactory.create(context)
    private val installListeners = mutableListOf<(Feature.Info?, Language?) -> Unit>()

    private fun SplitInstallSessionState.progress(): Int {
        return ((bytesDownloaded() / totalBytesToDownload().toFloat()) * 100).roundToInt()
    }

    private fun <T : Feature<*>> handleDownloadingState(
        state: SplitInstallSessionState,
        featureType: KClass<T>?,
        language: Language?,
        onStateUpdate: (FeatureManager.InstallState) -> Unit
    ) {
        val progress = state.progress()
        onStateUpdate(
            FeatureManager.InstallState.Downloading(
                progress = progress,
                featureInfo = featureType?.info(context),
                language = language,
                sessionId = state.sessionId()
            )
        )
    }

    private fun <T : Feature<*>> handleInstallingState(
        state: SplitInstallSessionState,
        featureType: KClass<T>?,
        language: Language?,
        onStateUpdate: (FeatureManager.InstallState) -> Unit
    ) {
        val progress = state.progress()
        onStateUpdate(
            FeatureManager.InstallState.Installing(
                progress = progress,
                featureInfo = featureType?.info(context),
                language = language,
                sessionId = state.sessionId()
            )
        )
    }

    private fun <T : Feature<*>> handleUserConfirmationRequired(
        state: SplitInstallSessionState,
        featureType: KClass<T>?,
        language: Language?,
        onStateUpdate: (FeatureManager.InstallState) -> Unit
    ) {
        onStateUpdate(
            FeatureManager.InstallState.RequiresUserConfirmation(
                sender = state.resolutionIntent()?.intentSender,
                featureInfo = featureType?.info(context),
                language = language,
                sessionId = state.sessionId()
            )
        )
    }

    private fun <T : Feature<*>> handleInstalled(
        state: SplitInstallSessionState,
        featureType: KClass<T>?,
        language: Language?,
        onStateUpdate: (FeatureManager.InstallState) -> Unit
    ) {
        onStateUpdate(
            FeatureManager.InstallState.Installed(
                featureInfo = featureType?.info(context),
                language = language,
                sessionId = state.sessionId()
            )
        )
    }

    private fun <T : Feature<*>> handleFailed(
        state: SplitInstallSessionState,
        featureType: KClass<T>?,
        language: Language?,
        onStateUpdate: (FeatureManager.InstallState) -> Unit
    ) {
        onStateUpdate(
            FeatureManager.InstallState.Failed(
                code = state.errorCode(),
                featureInfo = featureType?.info(context),
                language = language,
                sessionId = state.sessionId()
            )
        )
    }

    private fun <T : Feature<*>> handleCanceled(
        state: SplitInstallSessionState,
        featureType: KClass<T>?,
        language: Language?,
        onStateUpdate: (FeatureManager.InstallState) -> Unit
    ) {
        onStateUpdate(
            FeatureManager.InstallState.Canceled(
                featureInfo = featureType?.info(context),
                language = language,
                sessionId = state.sessionId()
            )
        )
    }

    private fun doRequest(
        featureType: KClass<out Feature<*>>?,
        language: Language?,
        request: SplitInstallRequest,
        onStateUpdate: (FeatureManager.InstallState) -> Unit
    ) {
        val installStateUpdateListener = object : SplitInstallStateUpdatedListener {
            override fun onStateUpdate(state: SplitInstallSessionState) {
                when (state.status()) {
                    SplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION -> {
                        handleUserConfirmationRequired(state, featureType, language, onStateUpdate)
                    }
                    SplitInstallSessionStatus.DOWNLOADING -> {
                        handleDownloadingState(state, featureType, language, onStateUpdate)
                    }
                    SplitInstallSessionStatus.INSTALLING, SplitInstallSessionStatus.DOWNLOADED -> {
                        handleInstallingState(state, featureType, language, onStateUpdate)
                    }
                    SplitInstallSessionStatus.INSTALLED -> {
                        splitInstallManager.unregisterListener(this)
                        installListeners.forEach { listener ->
                            listener(
                                featureType?.info(context),
                                language
                            )
                        }
                        handleInstalled(state, featureType, language, onStateUpdate)
                    }
                    SplitInstallSessionStatus.FAILED -> {
                        splitInstallManager.unregisterListener(this)
                        handleFailed(state, featureType, language, onStateUpdate)
                    }
                    SplitInstallSessionStatus.CANCELED -> {
                        splitInstallManager.unregisterListener(this)
                        handleCanceled(state, featureType, language, onStateUpdate)
                    }
                }
            }
        }

        splitInstallManager.registerListener(installStateUpdateListener)
        val task = splitInstallManager.startInstall(request)
        task.addOnCompleteListener {
            splitInstallManager.unregisterListener(installStateUpdateListener)
        }
        task.addOnFailureListener {
            splitInstallManager.unregisterListener(installStateUpdateListener)
        }
    }

    override fun <T : Feature<*>> installFeature(
        featureType: KClass<T>,
        onStateUpdate: (FeatureManager.InstallState) -> Unit
    ) {
        val request = SplitInstallRequest.newBuilder()
            .addModule(featureType.info(context).id)
            .build()

        doRequest(featureType, null, request, onStateUpdate)
    }

    override fun <T : Language> installLanguage(
        languageType: KClass<T>,
        onStateUpdate: (FeatureManager.InstallState) -> Unit
    ) {
        val language = Language.create(context, languageType)
        val request = SplitInstallRequest.newBuilder()
            .addLanguage(language.locale)
            .build()

        doRequest(null, language, request, onStateUpdate)
    }

    override fun registerInstallListener(listener: (Feature.Info?, Language?) -> Unit) {
        installListeners.add(listener)
    }

    override fun unregisterInstallListener(listener: (Feature.Info?, Language?) -> Unit) {
        installListeners.remove(listener)
    }

    override fun <T : Feature<*>> isFeatureInstalled(featureType: KClass<T>): Boolean {
        return splitInstallManager.installedModules.contains(featureType.info(context).id)
    }

    override fun isLanguageInstalled(languageType: KClass<out Language>): Boolean {
        return splitInstallManager.installedLanguages.map { it.toLowerCase() }.contains(
            Language.create(
                context,
                languageType
            ).code
        )
    }

    override fun cancelInstall(sessionId: Int) {
        splitInstallManager.cancelInstall(sessionId)
    }
}