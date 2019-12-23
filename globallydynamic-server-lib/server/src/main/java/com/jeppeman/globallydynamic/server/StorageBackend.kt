package com.jeppeman.globallydynamic.server

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.ObjectMetadata
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import com.jeppeman.globallydynamic.server.extensions.deleteCompletely
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.*

interface StorageBackend {
    fun storeFile(name: String, contentType: String, inputStream: InputStream)
    fun retrieveFile(name: String): Path?
    fun deleteFile(name: String)
    fun exists(name: String): Boolean

    companion object {
        val LOCAL_DEFAULT: LocalStorageBackend = LocalStorageBackend.builder().build()
    }
}

class LocalStorageBackend private constructor(
    private val baseStoragePath: Path
) : StorageBackend {

    override fun storeFile(name: String, contentType: String, inputStream: InputStream) {
        baseStoragePath.toFile().mkdirs()
        inputStream.use {
            val file = baseStoragePath.resolve(name).apply { toFile().createNewFile() }
            Files.copy(it, file, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    override fun retrieveFile(name: String): Path? {
        return baseStoragePath.resolve(name).toFile().takeIf(File::exists)?.toPath()
    }

    override fun deleteFile(name: String) {
        baseStoragePath.resolve(name).deleteCompletely()
    }

    override fun exists(name: String): Boolean = Files.exists(baseStoragePath.resolve(name))

    override fun toString(): String {
        return "LocalStorageBackend(baseStoragePath=$baseStoragePath)"
    }

    fun newBuilder(): Builder = Builder(this)

    class Builder internal constructor() {
        @set:JvmSynthetic
        var baseStoragePath: Path = Paths.get(System.getProperty("user.dir"))

        internal constructor(localStorageBackend: LocalStorageBackend) : this() {
            baseStoragePath = localStorageBackend.baseStoragePath
        }

        fun setBaseStoragePath(baseStoragePath: Path) = apply { this.baseStoragePath = baseStoragePath }

        fun build(): LocalStorageBackend = LocalStorageBackend(if (baseStoragePath.isAbsolute) {
            baseStoragePath
        } else {
            Paths.get(System.getProperty("user.dir"), baseStoragePath.toString())
        })
    }

    companion object {
        fun builder(): Builder = Builder()
    }
}

class GoogleCloudStorageBackend private constructor(
    private val bucketId: String,
    private val storage: Storage = StorageOptions.getDefaultInstance().service
) : StorageBackend {

    private fun String.asBlobId() = BlobId.of(bucketId, this)

    private fun createTempFile(name: String) = Files.createTempDirectory("GoogleCloudStorageBackend").resolve(name)

    override fun storeFile(name: String, contentType: String, inputStream: InputStream) {
        val blobId = name.asBlobId()
        val blobInfo = BlobInfo.newBuilder(blobId).setContentType(contentType).build()
        try {
            storage.delete(blobId)
        } finally {
            inputStream.use {
                val writeChannel = storage.writer(blobInfo)
                val buffer = ByteArray(1024)
                var limit = 0
                while (inputStream.read(buffer).also { limit = it } >= 0) {
                    writeChannel.write(ByteBuffer.wrap(buffer, 0, limit))
                }
                writeChannel.close()
            }
        }
    }

    override fun retrieveFile(name: String): Path? {
        val blobId = name.asBlobId()
        val blob = storage.get(blobId)
        return if (blob != null) {
            createTempFile(name).apply(blob::downloadTo)
        } else {
            null
        }
    }

    override fun deleteFile(name: String) {
        val blobId = name.asBlobId()
        storage.delete(blobId)
    }

    override fun exists(name: String): Boolean = storage.get(name.asBlobId()) != null

    override fun toString(): String {
        return "GoogleCloudStorageBackend(bucketId=$bucketId)"
    }

    fun newBuilder(): Builder = Builder(this)

    class Builder internal constructor() {
        @set:JvmSynthetic
        var bucketId: String = ""

        internal constructor(googleCloudStorageBackend: GoogleCloudStorageBackend) : this() {
            bucketId = googleCloudStorageBackend.bucketId
        }

        fun setBucketId(bucketId: String) = apply { this.bucketId = bucketId }

        fun build(): GoogleCloudStorageBackend {
            val bucketId = requireNotNull(this.bucketId.takeIf(String::isNotBlank))
            return GoogleCloudStorageBackend(bucketId)
        }
    }

    companion object {
        fun builder(): Builder = Builder()
    }
}

class S3Backend(
    private val bucketId: String,
    private val s3: AmazonS3 = AmazonS3ClientBuilder.defaultClient()
) : StorageBackend {
    private fun createTempFile(name: String) = Files.createTempDirectory("S3Backend").resolve(name)

    override fun storeFile(name: String, contentType: String, inputStream: InputStream) {
        val tempFile = createTempFile(UUID.randomUUID().toString())
        inputStream.use {
            tempFile.toFile().outputStream().use { outputStream ->
                it.copyTo(outputStream)
            }
        }
        tempFile.toFile().inputStream().use { fileInputStream ->
            val metaData = ObjectMetadata().apply {
                setContentType(contentType)
                contentLength = tempFile.toFile().length()
            }
            s3.putObject(bucketId, name, fileInputStream, metaData)
        }

        tempFile.deleteCompletely()
    }

    override fun retrieveFile(name: String): Path? {
        val tempFile = createTempFile(name)
        val s3Object = s3.getObject(bucketId, name)
        s3Object.objectContent.use { inputStream ->
            tempFile.toFile().outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }

        return tempFile
    }

    override fun deleteFile(name: String) {
        s3.deleteObject(bucketId, name)
    }

    override fun exists(name: String): Boolean = s3.doesObjectExist(bucketId, name)

    override fun toString(): String {
        return "S3Backend(bucketId=$bucketId)"
    }

    class Builder internal constructor() {
        @set:JvmSynthetic
        var bucketId: String = ""

        internal constructor(s3Backend: S3Backend) : this() {
            bucketId = s3Backend.bucketId
        }

        fun setBucketId(bucketId: String) = apply { this.bucketId = bucketId }

        fun build(): S3Backend {
            val bucketId = requireNotNull(this.bucketId.takeIf(String::isNotBlank))
            return S3Backend(bucketId)
        }
    }

    companion object {
        fun builder(): Builder = Builder()
    }
}