package com.jeppeman.locallydynamic.server

import com.android.bundle.Devices
import com.android.tools.build.bundletool.commands.BuildApksCommand
import com.android.tools.build.bundletool.commands.ExtractApksCommand
import com.android.tools.build.bundletool.model.Password
import com.android.tools.build.bundletool.model.SigningConfiguration
import com.android.tools.build.bundletool.model.exceptions.CommandExecutionException
import com.google.common.collect.ImmutableSet
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.jeppeman.locallydynamic.server.dto.toDeviceSpec
import com.jeppeman.locallydynamic.server.dto.toDeviceSpecDto
import com.jeppeman.locallydynamic.server.extensions.deleteCompletely
import com.jeppeman.locallydynamic.server.extensions.stackTraceToString
import com.jeppeman.locallydynamic.server.extensions.zip
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.KeyStore
import java.util.*

internal interface BundleManager {
    fun generateCompressedSplits(
            applicationId: String,
            version: Int,
            variant: String,
            deviceSpec: Devices.DeviceSpec,
            features: Array<String>,
            languages: Array<String>
    ): Result

    fun storeBundle(
            applicationId: String,
            version: Int,
            variant: String,
            signingConfig: String,
            bundleInputStream: InputStream,
            keyStoreInputStream: InputStream
    ): Result

    sealed class Result(val message: String) {
        class Success(val path: Path) : Result("Success")
        sealed class Error(message: String) : Result(message) {
            class BundleNotFound(filename: String) : Error("Bundle with filename $filename was not found")
            class BuildApksFailure(message: String) : Error("Failed to build APKs: $message")
            class ExtractApksFailure(message: String) : Error("Failed to extract APKs: $message")
            object MissingFeaturesAndLanguages : Error("Features or languages must not be empty")
            object KeystorePassMissing : Error("Keystore pass is missing")
            object KeyPassMissing : Error("Key pass is missing")
            object KeyAliasMissing : Error("Key alias is missing")
        }
    }

    companion object : (StorageBackend, Logger, Gson) -> BundleManager {
        override fun invoke(storageBackend: StorageBackend, logger: Logger, gson: Gson): BundleManager = BundleManagerImpl(
                storageBackend = storageBackend,
                logger = logger,
                gson = gson
        )
    }
}

internal class BundleManagerImpl(
        private val gson: Gson,
        private val storageBackend: StorageBackend,
        private val logger: Logger
) : BundleManager {
    private fun getFinalFileName(
            applicationId: String,
            version: Int,
            variant: String,
            extension: String
    ): String = "${applicationId}_${variant}_$version.$extension"

    internal fun extractApks(
            apksArchivePath: Path,
            outputDirectory: Path,
            deviceSpec: Devices.DeviceSpec,
            features: Array<String>
    ): List<Path> = ExtractApksCommand.builder()
            .setApksArchivePath(apksArchivePath)
            .setOutputDirectory(outputDirectory)
            .setDeviceSpec(deviceSpec)
            .apply {
                if (features.isNotEmpty()) {
                    setModules(ImmutableSet.copyOf(features))
                }
            }
            .build()
            .execute()

    internal fun buildApks(
            bundlePath: Path,
            outputDirectory: Path,
            keystorePath: Path,
            keystorePass: String,
            keyAlias: String,
            keyPass: String
    ): Path = BuildApksCommand.builder()
            .setBundlePath(bundlePath)
            .setOutputFile(outputDirectory)
            .setSigningConfiguration(SigningConfiguration.extractFromKeystore(
                    keystorePath,
                    keyAlias,
                    Optional.of(Password { KeyStore.PasswordProtection(keystorePass.toCharArray()) }),
                    Optional.of(Password { KeyStore.PasswordProtection(keyPass.toCharArray()) })
            ))
            .build()
            .execute()

    override fun generateCompressedSplits(
            applicationId: String,
            version: Int,
            variant: String,
            deviceSpec: Devices.DeviceSpec,
            features: Array<String>,
            languages: Array<String>
    ): BundleManager.Result {
        if (features.isEmpty() && languages.isEmpty()) {
            return BundleManager.Result.Error.MissingFeaturesAndLanguages
        }

        val apkSetFilename = getFinalFileName(applicationId, version, variant, "apks")
        val apkSet = storageBackend.retrieveFile(apkSetFilename)
                ?: return BundleManager.Result.Error.BundleNotFound(apkSetFilename)
        val tempDir = Files.createTempDirectory("${applicationId}_${version}_$variant")

        val splitsPaths = try {
            extractApks(
                    apksArchivePath = apkSet,
                    outputDirectory = tempDir,
                    deviceSpec = deviceSpec.toDeviceSpecDto().run {
                        copy(supportedLocales = supportedLocales + languages)
                    }.toDeviceSpec(),
                    features = features)
                    .filter { apk ->
                        val apkFilename = apk.toFile().name
                        val keep = features.any { featureToInstall ->
                            apkFilename.startsWith(featureToInstall)
                        } || languages.any { languageToInstall ->
                            apkFilename.endsWith("-$languageToInstall.apk")
                        }

                        if (keep) {
                            true
                        } else {
                            apk.deleteCompletely()
                            false
                        }
                    }
        } catch (commandExecutionException: CommandExecutionException) {
            return BundleManager.Result.Error.ExtractApksFailure(
                    commandExecutionException.message ?: commandExecutionException.stackTraceToString())
        }

        logger.i("Extracted the following APKs: ${splitsPaths.joinToString(", ") { it.fileName.toString() }}")

        val extractedSplitsZipPath = tempDir.resolve(
                (features + languages).joinToString(
                        separator = "_",
                        postfix = "_extracted.zip"
                )
        ).apply {
            toFile().zip(*splitsPaths.map(Path::toFile).toTypedArray())
        }

        return BundleManager.Result.Success(extractedSplitsZipPath)
    }

    override fun storeBundle(
            applicationId: String,
            version: Int,
            variant: String,
            signingConfig: String,
            bundleInputStream: InputStream,
            keyStoreInputStream: InputStream
    ): BundleManager.Result {
        val tempDir = Files.createTempDirectory("${applicationId}_${version}_$variant")
        val outputDir = tempDir.resolve("bundle.apks")
        val keyStoreTempFile = tempDir.resolve("temp.keystore")
        Files.copy(keyStoreInputStream, keyStoreTempFile, StandardCopyOption.REPLACE_EXISTING)
        val bundleTempFile = tempDir.resolve("temp.aab")
        Files.copy(bundleInputStream, bundleTempFile, StandardCopyOption.REPLACE_EXISTING)
        val signingConfigJson = gson.fromJson(signingConfig, JsonObject::class.java)
        val keystorePass = signingConfigJson.get("storePassword")?.asString
                ?: return BundleManager.Result.Error.KeystorePassMissing
        val keyPass = signingConfigJson.get("keyPassword")?.asString
                ?: return BundleManager.Result.Error.KeyPassMissing
        val keyAlias = signingConfigJson.get("keyAlias")?.asString
                ?: return BundleManager.Result.Error.KeyAliasMissing

        val apkSetPath = try {
            buildApks(
                    bundlePath = bundleTempFile,
                    outputDirectory = outputDir,
                    keystorePath = keyStoreTempFile,
                    keystorePass = keystorePass,
                    keyPass = keyPass,
                    keyAlias = keyAlias
            )
        } catch (commandExecutionException: CommandExecutionException) {
            return BundleManager.Result.Error.BuildApksFailure(
                    commandExecutionException.message ?: commandExecutionException.stackTraceToString())
        }

        val apkSetFileName = getFinalFileName(applicationId, version, variant, "apks")
        storageBackend.storeFile(apkSetFileName, "application/zip", apkSetPath.toFile().inputStream())

        val bundleFileName = getFinalFileName(applicationId, version, variant, "aab")
        storageBackend.storeFile(bundleFileName, "application/zip", bundleTempFile.toFile().inputStream())
        val signingConfigFileName = getFinalFileName(applicationId, version, variant, "json")
        storageBackend.storeFile(signingConfigFileName, "application/json", signingConfig.byteInputStream())
        val keystoreFileName = getFinalFileName(applicationId, version, variant, "keystore")
        storageBackend.storeFile(keystoreFileName, "application/octet-stream", keyStoreTempFile.toFile().inputStream())

        bundleTempFile.deleteCompletely()
        keyStoreTempFile.deleteCompletely()

        return BundleManager.Result.Success(apkSetPath)
    }
}