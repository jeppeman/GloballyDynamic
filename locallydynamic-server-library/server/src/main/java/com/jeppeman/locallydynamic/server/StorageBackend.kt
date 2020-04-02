package com.jeppeman.locallydynamic.server

import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import com.jeppeman.locallydynamic.server.extensions.deleteCompletely
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

interface StorageBackend {
    fun storeFile(name: String, contentType: String, inputStream: InputStream)
    fun retrieveFile(name: String): Path?
    fun deleteFile(name: String)

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