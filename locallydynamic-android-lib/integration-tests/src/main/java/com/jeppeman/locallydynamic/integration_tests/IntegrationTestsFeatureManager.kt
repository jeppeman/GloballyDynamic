package com.jeppeman.locallydynamic.integration_tests

import android.content.Context
import android.content.IntentSender
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.google.android.play.core.splitinstall.SplitInstallSessionState
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import java.util.*
import kotlin.math.roundToInt
import kotlin.reflect.KClass

inline fun <reified T : Feature<D>, D> IntegrationTestsFeatureManager.getFeature(
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

inline fun <reified T : Feature<*>> IntegrationTestsFeatureManager.installFeature(
    noinline onStateUpdate: (IntegrationTestsFeatureManager.InstallState) -> Unit
) = installFeature(T::class, onStateUpdate)

inline fun <reified T : Feature<*>> IntegrationTestsFeatureManager.isFeatureInstalled(): Boolean =
    isFeatureInstalled(T::class)

interface IntegrationTestsFeatureManager {
    fun <T : Feature<*>> installFeature(
        featureType: KClass<T>,
        onStateUpdate: (InstallState) -> Unit
    )

    fun <T : Feature<*>> isFeatureInstalled(featureType: KClass<T>): Boolean
    fun cancelInstall(sessionId: Int)
    fun registerInstallListener(listener: (Feature.Info) -> Unit)
    fun unregisterInstallListener(listener: (Feature.Info) -> Unit)

    sealed class InstallState(val featureInfo: Feature.Info, val sessionId: Int) {
        class Downloading(
            val progress: Int,
            featureInfo: Feature.Info,
            sessionId: Int
        ) : InstallState(featureInfo, sessionId)

        class Installing(
            val progress: Int,
            featureInfo: Feature.Info,
            sessionId: Int
        ) : InstallState(featureInfo, sessionId)

        class RequiresUserConfirmation(
            val sender: IntentSender?,
            featureInfo: Feature.Info,
            sessionId: Int
        ) : InstallState(featureInfo, sessionId)

        class Failed(
            val code: Int,
            featureInfo: Feature.Info,
            sessionId: Int
        ) : InstallState(featureInfo, sessionId)

        class Installed(
            featureInfo: Feature.Info,
            sessionId: Int
        ) : InstallState(featureInfo, sessionId)

        class Canceled(
            featureInfo: Feature.Info,
            sessionId: Int
        ) : InstallState(featureInfo, sessionId)
    }

    companion object : (Context) -> IntegrationTestsFeatureManager {
        private lateinit var instance: IntegrationTestsFeatureManager
        override fun invoke(context: Context): IntegrationTestsFeatureManager = if (Companion::instance.isInitialized) {
            instance
        } else {
            IntegrationTestsFeatureManagerImpl(context).apply {
                instance = this
            }
        }
    }
}

internal class IntegrationTestsFeatureManagerImpl(
    private val context: Context
) : IntegrationTestsFeatureManager {
    private val splitInstallManager: SplitInstallManager =
        (context as IntegrationTestsApplication).splitInstallManager
    private val installListeners = mutableListOf<(Feature.Info) -> Unit>()

    private fun SplitInstallSessionState.progress(): Int {
        return ((bytesDownloaded() / totalBytesToDownload().toFloat()) * 100).roundToInt()
    }

    private fun <T : Feature<*>> handleDownloadingState(
        state: SplitInstallSessionState,
        featureType: KClass<T>,
        onStateUpdate: (IntegrationTestsFeatureManager.InstallState) -> Unit
    ) {
        val progress = state.progress()
        onStateUpdate(
            IntegrationTestsFeatureManager.InstallState.Downloading(
                progress = progress,
                featureInfo = featureType.info(context),
                sessionId = state.sessionId()
            )
        )
    }

    private fun <T : Feature<*>> handleInstallingState(
        state: SplitInstallSessionState,
        featureType: KClass<T>,
        onStateUpdate: (IntegrationTestsFeatureManager.InstallState) -> Unit
    ) {
        val progress = state.progress()
        onStateUpdate(
            IntegrationTestsFeatureManager.InstallState.Installing(
                progress = progress,
                featureInfo = featureType.info(context),
                sessionId = state.sessionId()
            )
        )
    }

    private fun <T : Feature<*>> handleUserConfirmationRequired(
        state: SplitInstallSessionState,
        featureType: KClass<T>,
        onStateUpdate: (IntegrationTestsFeatureManager.InstallState) -> Unit
    ) {
        onStateUpdate(
            IntegrationTestsFeatureManager.InstallState.RequiresUserConfirmation(
                sender = state.resolutionIntent()?.intentSender,
                featureInfo = featureType.info(context),
                sessionId = state.sessionId()
            )
        )
    }

    private fun <T : Feature<*>> handleInstalled(
        state: SplitInstallSessionState,
        featureType: KClass<T>,
        onStateUpdate: (IntegrationTestsFeatureManager.InstallState) -> Unit
    ) {
        onStateUpdate(
            IntegrationTestsFeatureManager.InstallState.Installed(
                featureInfo = featureType.info(context),
                sessionId = state.sessionId()
            )
        )
    }

    private fun <T : Feature<*>> handleFailed(
        state: SplitInstallSessionState,
        featureType: KClass<T>,
        onStateUpdate: (IntegrationTestsFeatureManager.InstallState) -> Unit
    ) {
        onStateUpdate(
            IntegrationTestsFeatureManager.InstallState.Failed(
                code = state.errorCode(),
                featureInfo = featureType.info(context),
                sessionId = state.sessionId()
            )
        )
    }

    private fun <T : Feature<*>> handleCanceled(
        state: SplitInstallSessionState,
        featureType: KClass<T>,
        onStateUpdate: (IntegrationTestsFeatureManager.InstallState) -> Unit
    ) {
        onStateUpdate(
            IntegrationTestsFeatureManager.InstallState.Canceled(
                featureInfo = featureType.info(context),
                sessionId = state.sessionId()
            )
        )
    }

    override fun <T : Feature<*>> installFeature(
        featureType: KClass<T>,
        onStateUpdate: (IntegrationTestsFeatureManager.InstallState) -> Unit
    ) {
        val request = SplitInstallRequest.newBuilder()
            .addModule(featureType.info(context).id)
            .addLanguage(Locale.ITALIAN)
            .build()

        val installStateUpdateListener = object : SplitInstallStateUpdatedListener {
            override fun onStateUpdate(state: SplitInstallSessionState) {
                state.moduleNames().forEach { _ ->
                    when (state.status()) {
                        SplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION -> {
                            handleUserConfirmationRequired(state, featureType, onStateUpdate)
                        }
                        SplitInstallSessionStatus.DOWNLOADING -> {
                            handleDownloadingState(state, featureType, onStateUpdate)
                        }
                        SplitInstallSessionStatus.INSTALLING, SplitInstallSessionStatus.DOWNLOADED -> {
                            handleInstallingState(state, featureType, onStateUpdate)
                        }
                        SplitInstallSessionStatus.INSTALLED -> {
                            splitInstallManager.unregisterListener(this)
                            installListeners.forEach { listener ->
                                listener(featureType.info(context))
                            }
                            handleInstalled(state, featureType, onStateUpdate)
                        }
                        SplitInstallSessionStatus.FAILED -> {
                            splitInstallManager.unregisterListener(this)
                            handleFailed(state, featureType, onStateUpdate)
                        }
                        SplitInstallSessionStatus.CANCELED -> {
                            splitInstallManager.unregisterListener(this)
                            handleCanceled(state, featureType, onStateUpdate)
                        }
                    }
                }
            }
        }

        splitInstallManager.registerListener(installStateUpdateListener)
        val task = splitInstallManager.startInstall(request)
        task.addOnCompleteListener {
            splitInstallManager.unregisterListener(installStateUpdateListener)
        }
        task.addOnFailureListener{
            splitInstallManager.unregisterListener(installStateUpdateListener)
        }
    }

    override fun registerInstallListener(listener: (Feature.Info) -> Unit) {
        installListeners.add(listener)
    }

    override fun unregisterInstallListener(listener: (Feature.Info) -> Unit) {
        installListeners.remove(listener)
    }

    override fun <T : Feature<*>> isFeatureInstalled(featureType: KClass<T>): Boolean {
        return splitInstallManager.installedModules.contains(featureType.info(context).id)
    }

    override fun cancelInstall(sessionId: Int) {
        splitInstallManager.cancelInstall(sessionId)
    }
}