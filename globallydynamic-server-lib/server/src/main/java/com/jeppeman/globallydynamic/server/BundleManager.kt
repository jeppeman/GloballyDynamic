package com.jeppeman.globallydynamic.server

import com.android.bundle.Devices
import com.android.tools.build.bundletool.commands.BuildApksCommand
import com.android.tools.build.bundletool.commands.ExtractApksCommand
import com.android.tools.build.bundletool.model.Password
import com.android.tools.build.bundletool.model.SigningConfiguration
import com.android.tools.build.bundletool.model.exceptions.CommandExecutionException
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.jeppeman.globallydynamic.server.dto.toDeviceSpec
import com.jeppeman.globallydynamic.server.dto.toDeviceSpecDto
import com.jeppeman.globallydynamic.server.extensions.deleteCompletely
import com.jeppeman.globallydynamic.server.extensions.stackTraceToString
import com.jeppeman.globallydynamic.server.extensions.zip
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.KeyStore
import java.security.MessageDigest
import java.security.cert.Certificate
import java.util.*
import java.util.Locale


internal interface BundleManager {
    fun generateCompressedSplits(
        applicationId: String,
        version: Int,
        variant: String,
        deviceSpec: Devices.DeviceSpec,
        features: Array<String>,
        languages: Array<String>,
        includeMissing: Boolean
    ): Result

    fun storeBundle(
        applicationId: String,
        version: Int,
        variant: String,
        signingConfig: String,
        bundleInputStream: InputStream,
        keyStoreInputStream: InputStream
    ): Result

    fun validateSignature(
        signature: String,
        applicationId: String,
        version: Int,
        variant: String
    ): Result

    sealed class Result(val message: String) {
        class Success(val path: Path) : Result("Success")
        sealed class Error(message: String) : Result(message) {
            class BundleNotFound(filename: String) : Error("Bundle with filename $filename was not found")
            class BuildApksFailure(message: String) : Error("Failed to build APKs: $message")
            class ExtractApksFailure(message: String) : Error("Failed to extract APKs: $message")
            class BundleExists(message: String) : Error("The bundle already exists: $message")
            class SignatureMismatch(signature: String) : Error("The provided signature \"$signature\" does not match the one on the server")
            class SignatureNotFound(signature: String) : Error("The signature \"${signature}\" does not exist")
            object MissingFeaturesAndLanguages : Error("Features or languages must not be empty")
            object KeystorePassMissing : Error("Keystore pass is missing")
            object KeyPassMissing : Error("Key pass is missing")
            object KeyAliasMissing : Error("Key alias is missing")
        }
    }

    companion object : (StorageBackend, Logger, Gson, Boolean) -> BundleManager {
        override fun invoke(
            storageBackend: StorageBackend,
            logger: Logger,
            gson: Gson,
            overrideExistingBundles: Boolean
        ): BundleManager = BundleManagerImpl(
            storageBackend = storageBackend,
            logger = logger,
            gson = gson,
            overrideExistingBundles = overrideExistingBundles
        )
    }
}

internal class BundleManagerImpl(
    private val gson: Gson,
    private val storageBackend: StorageBackend,
    private val logger: Logger,
    private val overrideExistingBundles: Boolean
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
    ): ImmutableList<Path> = ExtractApksCommand.builder()
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
        languages: Array<String>,
        includeMissing: Boolean
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
                features = features
            ).filter { apk ->
                val apkFilename = apk.toFile().name
                val keep = features.any { featureToInstall ->
                    apkFilename.startsWith(featureToInstall)
                } || languages.any { languageToInstall ->
                    apkFilename.endsWith("-$languageToInstall.apk")
                } || (includeMissing
                    && apkFilename.startsWith("base")
                    && !apkFilename.endsWith("-master.apk"))

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
        val bundleFileName = getFinalFileName(applicationId, version, variant, "aab")

        if (!overrideExistingBundles && storageBackend.exists(bundleFileName)) {
            return BundleManager.Result.Error.BundleExists(bundleFileName)
        }

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
        } catch (exception: Exception) {
            return BundleManager.Result.Error.BuildApksFailure(
                exception.message ?: exception.stackTraceToString())
        }

        val apkSetFileName = getFinalFileName(applicationId, version, variant, "apks")
        storageBackend.storeFile(apkSetFileName, "application/zip", apkSetPath.toFile().inputStream())

        storageBackend.storeFile(bundleFileName, "application/zip", bundleTempFile.toFile().inputStream())
        val signingConfigFileName = getFinalFileName(applicationId, version, variant, "json")
        storageBackend.storeFile(signingConfigFileName, "application/json", signingConfig.byteInputStream())
        val keystoreFileName = getFinalFileName(applicationId, version, variant, "keystore")
        storageBackend.storeFile(keystoreFileName, "application/octet-stream", keyStoreTempFile.toFile().inputStream())

        bundleTempFile.deleteCompletely()
        keyStoreTempFile.deleteCompletely()

        return BundleManager.Result.Success(apkSetPath)
    }

    override fun validateSignature(
        signature: String,
        applicationId: String,
        version: Int,
        variant: String): BundleManager.Result {
        val keystoreFileName = getFinalFileName(applicationId, version, variant, "keystore")
        val keyStoreFile = storageBackend.retrieveFile(keystoreFileName)

        val signingConfigFileName = getFinalFileName(applicationId, version, variant, "json")
        val signingConfig = storageBackend.retrieveFile(signingConfigFileName)

        if (keyStoreFile?.let { Files.exists(it) } != true || signingConfig?.let { Files.exists(it) } != true) {
            return BundleManager.Result.Error.SignatureNotFound(signature)
        }

        val signingConfigJson = gson.fromJson(signingConfig.toFile().readText(), JsonObject::class.java)
        val keystorePass = signingConfigJson.get("storePassword")?.asString
            ?: return BundleManager.Result.Error.KeystorePassMissing
        val keyAlias = signingConfigJson.get("keyAlias")?.asString
            ?: return BundleManager.Result.Error.KeyAliasMissing

        val keyStore = KeyStore.getInstance("JKS")

        keyStore.load(keyStoreFile.toFile().inputStream(), keystorePass.toCharArray())
        val chain = keyStore.getCertificateChain(keyAlias)

        val fingerPrint = getFingerPrintFromSignature(chain)

        logger.i("Validating signatures, provided: $signature, stored: $fingerPrint")

        if (fingerPrint != signature) {
            logger.e("Signature mismatch, provided ${signature}, stored: $fingerPrint")
            return BundleManager.Result.Error.SignatureMismatch(signature)
        }

        logger.i("Signature valid")

        return BundleManager.Result.Success(keyStoreFile)
    }
}

private fun getFingerPrintFromSignature(signatures: Array<Certificate>): String? {
    var hashKey: String? = null
    val stringBuilder = StringBuilder()
    for (signature in signatures) {
        val messageDigest = MessageDigest.getInstance("SHA-1")
        messageDigest.update(signature.encoded)
        for (b in messageDigest.digest()) {
            stringBuilder.append(String.format("%02x", b.toInt() and 0xff)).append(':')
        }
        hashKey = stringBuilder.toString().toUpperCase(Locale.getDefault())
        hashKey = hashKey.substring(0, hashKey.length - 1)
    }
    return hashKey
}
