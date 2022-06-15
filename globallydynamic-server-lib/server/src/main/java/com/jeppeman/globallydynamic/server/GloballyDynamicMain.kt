package com.jeppeman.globallydynamic.server

import java.nio.file.Path
import java.nio.file.Paths

private const val ARG_PORT = "--port"
private const val ARG_USERNAME = "--username"
private const val ARG_PASSWORD = "--password"
private const val ARG_HTTPS_REDIRECT = "--https-redirect"
private const val ARG_STORAGE_BACKEND = "--storage-backend"
private const val ARG_LOCAL_STORAGE_PATH = "--local-storage-path"
private const val ARG_GCP_BUCKET_ID = "--gcp-bucket-id"
private const val ARG_S3_BUCKET_ID = "--s3-bucket-id"
private const val ARG_OVERRIDE_EXISTING_BUNDLES = "--override-existing-bundles"
private const val ARG_VALIDATE_SIGNATURE_ON_DOWNLOAD = "--validate-signature-on-download"
private const val ARG_HOST_ADDRESS = "--host-address"

private const val ENV_VAR_PREFIX = "GLOBALLY_DYNAMIC_"
private const val ENV_PORT = "${ENV_VAR_PREFIX}PORT"
private const val ENV_USERNAME = "${ENV_VAR_PREFIX}USERNAME"
private const val ENV_PASSWORD = "${ENV_VAR_PREFIX}PASSWORD"
private const val ENV_HTTPS_REDIRECT = "${ENV_VAR_PREFIX}HTTPS_REDIRECT"
private const val ENV_STORAGE_BACKEND = "${ENV_VAR_PREFIX}STORAGE_BACKEND"
private const val ENV_LOCAL_STORAGE_PATH = "${ENV_VAR_PREFIX}LOCAL_STORAGE_PATH"
private const val ENV_GCP_BUCKET_ID = "${ENV_VAR_PREFIX}GCP_BUCKET_ID"
private const val ENV_S3_BUCKET_ID = "${ENV_VAR_PREFIX}S3_BUCKET_ID"
private const val ENV_OVERRIDE_EXISTING_BUNDLES = "${ENV_VAR_PREFIX}OVERRIDE_EXISTING_BUNDLES"
private const val ENV_VALIDATE_SIGNATURE_ON_DOWNLOAD = "${ENV_VAR_PREFIX}VALIDATE_SIGNATURE_ON_DOWNLOAD"
private const val ENV_HOST_ADDRESS = "${ENV_VAR_PREFIX}HOST_ADDRESS"

internal fun GloballyDynamicServer.Configuration.fromArgs(args: Array<String>): GloballyDynamicServer.Configuration {
    val configurationBuilder = newBuilder()
    fun tryGetArgValue(argName: String, index: Int) = try {
        args[index]
    } catch (exception: Exception) {
        throw IllegalArgumentException("No value provided for argument $argName")
    }

    var storageBackendArg: String? = null
    var localStoragePathArg: String? = null
    var gcpBucketIdArg: String? = null
    var s3BucketIdArg: String? = null

    for (i in 0 until args.size step 2) {
        when (val argName = args[i]) {
            ARG_PORT -> {
                val argValue = tryGetArgValue(ARG_PORT, i + 1)
                configurationBuilder.port = try {
                    argValue.toInt()
                } catch (numberFormatException: NumberFormatException) {
                    throw IllegalArgumentException("Expected a number as value for argument" +
                        " $ARG_PORT, got $argValue")
                }
            }
            ARG_USERNAME -> {
                configurationBuilder.username = tryGetArgValue(ARG_USERNAME, i + 1)
            }
            ARG_PASSWORD -> {
                configurationBuilder.password = tryGetArgValue(ARG_PASSWORD, i + 1)
            }
            ARG_HTTPS_REDIRECT -> {
                configurationBuilder.httpsRedirect = tryGetArgValue(ARG_HTTPS_REDIRECT, i + 1).toBoolean()
            }
            ARG_STORAGE_BACKEND -> {
                storageBackendArg = tryGetArgValue(ARG_STORAGE_BACKEND, i + 1)
            }
            ARG_LOCAL_STORAGE_PATH -> {
                localStoragePathArg = tryGetArgValue(ARG_LOCAL_STORAGE_PATH, i + 1)
            }
            ARG_GCP_BUCKET_ID -> {
                gcpBucketIdArg = tryGetArgValue(ARG_GCP_BUCKET_ID, i + 1)
            }
            ARG_S3_BUCKET_ID -> {
                s3BucketIdArg = tryGetArgValue(ARG_S3_BUCKET_ID, i + 1)
            }
            ARG_OVERRIDE_EXISTING_BUNDLES -> {
                configurationBuilder.overrideExistingBundles = tryGetArgValue(ARG_OVERRIDE_EXISTING_BUNDLES, i + 1).toBoolean()
            }
            ARG_VALIDATE_SIGNATURE_ON_DOWNLOAD -> {
                configurationBuilder.validateSignatureOnDownload = tryGetArgValue(ARG_VALIDATE_SIGNATURE_ON_DOWNLOAD, i + 1).toBoolean()
            }
            ARG_HOST_ADDRESS -> {
                configurationBuilder.hostAddress = tryGetArgValue(ARG_HOST_ADDRESS, i + 1)
            }
            else -> throw IllegalArgumentException("Unrecognized argument $argName")
        }
    }

    val storageBackend = when (storageBackendArg) {
        "local" -> {
            LocalStorageBackend.builder()
                .apply { localStoragePathArg?.toPath()?.let(::setBaseStoragePath) }
                .build()
        }
        "gcp" -> {
            GoogleCloudStorageBackend.builder()
                .setBucketId(gcpBucketIdArg ?: throw IllegalArgumentException("gcp was given as a storage backend, " +
                    "but no value for argument $ARG_GCP_BUCKET_ID was provided"))
                .build()
        }
        "s3" -> {
            S3Backend.builder()
                .setBucketId(s3BucketIdArg ?: throw IllegalArgumentException("s3 was given as a storage backend, " +
                    "but no value for $ARG_S3_BUCKET_ID was provided"))
                .build()
        }
        null -> configurationBuilder.storageBackend
        else -> throw IllegalArgumentException("Unsupported storage backend \"$storageBackendArg\", available " +
            "alternatives are [\"local\", \"gcp\", \"s3\"]")
    }

    configurationBuilder.setStorageBackend(storageBackend)

    return configurationBuilder.build()
}

private fun String.toPath(): Path = if (Paths.get(this).isAbsolute) {
    Paths.get(this)
} else {
    Paths.get(System.getProperty("user.dir"), this)
}

private fun StorageBackend.Companion.fromEnvironment(): StorageBackend = when (val storageBackend = System.getenv(ENV_STORAGE_BACKEND)) {
    "local", null -> {
        System.getenv(ENV_LOCAL_STORAGE_PATH)?.let { localStoragePath ->
            LocalStorageBackend.builder()
                .setBaseStoragePath(localStoragePath.toPath())
                .build()
        } ?: LOCAL_DEFAULT
    }
    "gcp" -> {
        System.getenv(ENV_GCP_BUCKET_ID)?.let { gcpBucketId ->
            GoogleCloudStorageBackend.builder()
                .setBucketId(gcpBucketId)
                .build()
        } ?: throw IllegalArgumentException("gcp was given as a storage backend, but no value for $ENV_GCP_BUCKET_ID" +
            " was found")
    }
    "s3" -> {
        System.getenv(ENV_S3_BUCKET_ID)?.let { s3BucketId ->
            S3Backend.builder()
                .setBucketId(s3BucketId)
                .build()
        } ?: throw IllegalArgumentException("s3 was given as a storage backend, but no value for $ENV_S3_BUCKET_ID" +
            " was found")
    }
    else -> throw IllegalArgumentException("Unsupported storage backend \"$storageBackend\", available alternatives" +
        " are [\"local\", \"gcp\", \"s3\"]")
}

private fun GloballyDynamicServer.Configuration.Companion.fromEnvironment(): GloballyDynamicServer.Configuration = builder()
    .apply { System.getenv(ENV_PORT)?.toInt()?.let(::setPort) }
    .apply { System.getenv(ENV_USERNAME)?.let(::setUsername) }
    .apply { System.getenv(ENV_PASSWORD)?.let(::setPassword) }
    .apply { System.getenv(ENV_HOST_ADDRESS)?.let(::setHostAddress) }
    .apply { System.getenv(ENV_HTTPS_REDIRECT)?.toBoolean()?.let(::setHttpsRedirect) }
    .apply { System.getenv(ENV_OVERRIDE_EXISTING_BUNDLES)?.toBoolean()?.let(::setOverrideExistingBundles) }
    .apply { System.getenv(ENV_VALIDATE_SIGNATURE_ON_DOWNLOAD)?.toBoolean()?.let(::setValidateSignatureOnDownload) }
    .setStorageBackend(StorageBackend.fromEnvironment())
    .build()

fun main(args: Array<String>) {
    val configuration = GloballyDynamicServer.Configuration
        .fromEnvironment()
        .fromArgs(args)

    val server = GloballyDynamicServer(configuration)
    server.start()
    server.join()
}