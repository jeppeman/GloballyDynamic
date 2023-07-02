package com.jeppeman.globallydynamic.server

import com.android.bundle.Devices
import com.google.common.collect.ImmutableList
import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.jeppeman.globallydynamic.server.extensions.deleteCompletely
import com.jeppeman.globallydynamic.server.extensions.unzip
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever
import java.nio.file.Path

@RunWith(JUnitPlatform::class)
class BundleManagerImplTest {
    private lateinit var bundleManager: BundleManagerImpl
    private lateinit var spyStorageBackend: StorageBackend
    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun setUp() {
        spyStorageBackend = spy(LocalStorageBackend
            .builder()
            .setBaseStoragePath(tempDir)
            .build())
        bundleManager = spy(BundleManagerImpl(Gson(), spyStorageBackend, Logger(), false))
    }

    @AfterEach
    fun tearDown() {
        tempDir.deleteCompletely()
    }

    @Test
    fun whenFeaturesAndLanguagesAreEmpty_generateCompressedSplitsShouldReturnMissingFeaturesAndLanguagesError() {
        val compressedSplitsResult = bundleManager.generateCompressedSplits(
            applicationId = "applicationId",
            version = 1,
            variant = "variant",
            deviceSpec = Devices.DeviceSpec.getDefaultInstance(),
            languages = arrayOf(),
            features = arrayOf(),
            includeMissing = false
        )

        assertThat(compressedSplitsResult)
            .isInstanceOf(BundleManager.Result.Error.MissingFeaturesAndLanguages::class.java)
    }

    @Test
    fun whenBundleDoesNotExist_generateCompressedSplitsShouldReturnBundleNotFoundError() {
        val compressedSplitsResult = bundleManager.generateCompressedSplits(
            applicationId = "applicationId",
            version = 1,
            variant = "variant",
            deviceSpec = Devices.DeviceSpec.getDefaultInstance(),
            languages = arrayOf(""),
            features = arrayOf(""),
            includeMissing = false
        )

        assertThat(compressedSplitsResult)
            .isInstanceOf(BundleManager.Result.Error.BundleNotFound::class.java)
    }

    @Test
    fun whenSigningConfigDoesNotExist_generateCompressedSplitsShouldReturnSigningConfigNotFoundError() {
        val compressedSplitsResult = bundleManager.generateCompressedSplits(
            applicationId = "applicationId",
            version = 1,
            variant = "variant",
            deviceSpec = Devices.DeviceSpec.getDefaultInstance(),
            languages = arrayOf(""),
            features = arrayOf(""),
            includeMissing = false
        )

        assertThat(compressedSplitsResult)
            .isInstanceOf(BundleManager.Result.Error.BundleNotFound::class.java)
    }

    @Test
    fun generateCompressedSplits_shouldGenerateSplitsByLanguagesAndFeatures() {
        val applicationId = "application"
        val version = 23
        val variant = "variant"
        val signingConfig = "{storePassword: \"pass\", keyPassword: \"keypass\", keyAlias: \"keyalias\"}"
        val fakeBundle = tempDir.resolve("bundle.aab").toFile().apply {
            writeText("Hi, I am a little bundle")
        }
        val fakeKeystore = tempDir.resolve("bundle.keystore").toFile().apply {
            writeText("Hi, I am a little keystore")
        }
        val unzipDir = tempDir.resolve("unzipped_apks")
        val files = ImmutableList.of(
            tempDir.resolve("base-se.apk").apply {
                toFile().writeText("file1")
            },
            tempDir.resolve("feature-master.apk").apply {
                toFile().writeText("file2")
            },
            tempDir.resolve("file3").apply {
                toFile().writeText("file3")
            }
        )
        val apks = tempDir.resolve("temp.apks").apply {
            toFile().writeText("apks")
        }
        doReturn(apks).whenever(bundleManager).buildApks(any(), any(), any(), any(), any(), any())
        doReturn(files).whenever(bundleManager).extractApks(any(), any(), any(), any())
        bundleManager.storeBundle(
            applicationId,
            version,
            variant,
            signingConfig,
            fakeBundle.inputStream(),
            fakeKeystore.inputStream()
        )

        val compressedSplitsResult = bundleManager.generateCompressedSplits(
            applicationId = applicationId,
            version = version,
            variant = variant,
            deviceSpec = Devices.DeviceSpec.getDefaultInstance(),
            languages = arrayOf("se"),
            features = arrayOf("feature"),
            includeMissing = false
        ) as BundleManager.Result.Success
        compressedSplitsResult.path.toFile().unzip(unzipDir.toString())
        val unzippedSplits = unzipDir.toFile().listFiles()!!.toList().map { it.readText() }

        assertThat(unzippedSplits).contains("file1")
        assertThat(unzippedSplits).contains("file2")
        assertThat(unzippedSplits).doesNotContain("file3")
    }
}

