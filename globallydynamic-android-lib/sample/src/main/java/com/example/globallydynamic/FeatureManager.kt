package com.example.globallydynamic

import android.app.Activity
import android.content.Context
import com.jeppeman.globallydynamic.globalsplitinstall.*
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
                private val startConfirm: (GlobalSplitInstallSessionState, Activity, Int) -> Boolean,
                private val state: GlobalSplitInstallSessionState,
                featureInfo: Feature.Info? = null,
                language: Language? = null,
                sessionId: Int
        ) : InstallState(featureInfo, language, sessionId) {
            fun startConfirmationDialogForResult(activity: Activity, requestCode: Int) {
                startConfirm(state, activity, requestCode)
            }
        }

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
        override fun invoke(context: Context): FeatureManager = if (Companion::instance.isInitialized) {
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
    private val splitInstallManager: GlobalSplitInstallManager =
            GlobalSplitInstallManagerFactory.create(context, GlobalSplitInstallProvider.SELF_HOSTED)
    private val installListeners = mutableListOf<(Feature.Info?, Language?) -> Unit>()

    private fun GlobalSplitInstallSessionState.progress(): Int {
        return ((bytesDownloaded() / totalBytesToDownload().toFloat()) * 100).roundToInt()
    }

    private fun <T : Feature<*>> handleDownloadingState(
            state: GlobalSplitInstallSessionState,
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
            state: GlobalSplitInstallSessionState,
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
            state: GlobalSplitInstallSessionState,
            featureType: KClass<T>?,
            language: Language?,
            onStateUpdate: (FeatureManager.InstallState) -> Unit
    ) {
        onStateUpdate(
                FeatureManager.InstallState.RequiresUserConfirmation(
                        startConfirm = splitInstallManager::startConfirmationDialogForResult,
                        state = state,
                        featureInfo = featureType?.info(context),
                        language = language,
                        sessionId = state.sessionId()
                )
        )
    }

    private fun <T : Feature<*>> handleInstalled(
            sessionId: Int,
            featureType: KClass<T>?,
            language: Language?,
            onStateUpdate: (FeatureManager.InstallState) -> Unit
    ) {
        onStateUpdate(
                FeatureManager.InstallState.Installed(
                        featureInfo = featureType?.info(context),
                        language = language,
                        sessionId = sessionId
                )
        )
    }

    private fun <T : Feature<*>> handleFailed(
            errorCode: Int,
            featureType: KClass<T>?,
            language: Language?,
            onStateUpdate: (FeatureManager.InstallState) -> Unit
    ) {
        onStateUpdate(
                FeatureManager.InstallState.Failed(
                        code = errorCode,
                        featureInfo = featureType?.info(context),
                        language = language,
                        sessionId = -1
                )
        )
    }

    private fun <T : Feature<*>> handleCanceled(
            state: GlobalSplitInstallSessionState,
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
            request: GlobalSplitInstallRequest,
            onStateUpdate: (FeatureManager.InstallState) -> Unit
    ) {
        val installStateUpdateListener = object : GlobalSplitInstallUpdatedListener {
            override fun onStateUpdate(state: GlobalSplitInstallSessionState) {
                when (state.status()) {
                    GlobalSplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION -> {
                        handleUserConfirmationRequired(state, featureType, language, onStateUpdate)
                    }
                    GlobalSplitInstallSessionStatus.DOWNLOADING -> {
                        handleDownloadingState(state, featureType, language, onStateUpdate)
                    }
                    GlobalSplitInstallSessionStatus.INSTALLING, GlobalSplitInstallSessionStatus.DOWNLOADED -> {
                        handleInstallingState(state, featureType, language, onStateUpdate)
                    }
                    GlobalSplitInstallSessionStatus.INSTALLED -> {
                        splitInstallManager.unregisterListener(this)
                        installListeners.forEach { listener ->
                            listener(
                                    featureType?.info(context),
                                    language
                            )
                        }
                        handleInstalled(state.sessionId(), featureType, language, onStateUpdate)
                    }
                    GlobalSplitInstallSessionStatus.CANCELED -> {
                        splitInstallManager.unregisterListener(this)
                        handleCanceled(state, featureType, language, onStateUpdate)
                    }
                }
            }
        }

        splitInstallManager.registerListener(installStateUpdateListener)
        splitInstallManager.startInstall(request)
                .addOnSuccessListener { sessionId ->
                    if (sessionId == 0) {
                        handleInstalled(sessionId, featureType, language, onStateUpdate)
                    }
                }
                .addOnFailureListener {
                    splitInstallManager.unregisterListener(installStateUpdateListener)
                    val errorCode = (it as? GlobalSplitInstallException)?.errorCode ?: -1
                    handleFailed(errorCode, featureType, language, onStateUpdate)
                }
    }

    override fun <T : Feature<*>> installFeature(
            featureType: KClass<T>,
            onStateUpdate: (FeatureManager.InstallState) -> Unit
    ) {
        val request = GlobalSplitInstallRequest.newBuilder()
                .addModule(featureType.info(context).id)
                .build()

        doRequest(featureType, null, request, onStateUpdate)
    }

    override fun <T : Language> installLanguage(
            languageType: KClass<T>,
            onStateUpdate: (FeatureManager.InstallState) -> Unit
    ) {
        val language = Language.create(context, languageType)
        val request = GlobalSplitInstallRequest.newBuilder()
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