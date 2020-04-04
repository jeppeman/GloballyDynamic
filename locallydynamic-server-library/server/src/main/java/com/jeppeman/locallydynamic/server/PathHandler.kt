package com.jeppeman.locallydynamic.server

import com.google.gson.Gson
import com.jeppeman.locallydynamic.server.dto.DeviceSpecDto
import com.jeppeman.locallydynamic.server.extensions.readString
import org.eclipse.jetty.http.HttpMethod
import org.eclipse.jetty.http.HttpStatus
import org.eclipse.jetty.server.Request
import javax.servlet.MultipartConfigElement
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.Part


internal interface PathHandler {
    val path: String
    val authRequired: Boolean get() = true

    fun HttpServletRequest?.requireQueryParam(queryParam: String): Array<String> {
        return this?.getQueryParam(queryParam)
                ?: throw HttpException(HttpStatus.BAD_REQUEST_400, "Missing required query parameter: $queryParam")
    }

    fun HttpServletRequest?.getQueryParam(queryParam: String): Array<String>? {
        return this?.parameterMap?.get(queryParam)
    }

    fun HttpServletRequest?.requirePart(
            name: String
    ): Part {
        return this?.parts?.find { part -> part.name == name }
                ?: throw HttpException(HttpStatus.BAD_REQUEST_400, "Missing required part: $name")
    }

    @Throws(HttpException::class)
    fun handle(request: HttpServletRequest?, response: HttpServletResponse?)
}

internal class DownloadSplitsPathHandler(
        private val bundleManager: BundleManager,
        private val deviceManager: DeviceManager,
        private val logger: Logger
) : PathHandler {
    override val path: String = "download"

    override fun handle(request: HttpServletRequest?, response: HttpServletResponse?) {
        val deviceIdParam = request.requireQueryParam("device-id")
        val applicationIdParam = request.requireQueryParam("application-id")
        val versionParam = request.requireQueryParam("version")
        val variantParam = request.requireQueryParam("variant")
        val throttle = request.getQueryParam("throttle")
        val featuresToInstallParam = request.getQueryParam("features") ?: arrayOf()
        val languagesToInstallParam = request.getQueryParam("languages") ?: arrayOf()

        val version = try {
            versionParam.first().toInt()
        } catch (exception: Exception) {
            throw HttpException(HttpStatus.BAD_REQUEST_400, "Expected version to be an integer, " +
                    "got ${versionParam.joinToString(",")}")
        }

        if (featuresToInstallParam.isEmpty() && languagesToInstallParam.isEmpty()) {
            throw HttpException(HttpStatus.BAD_REQUEST_400, "No features or languages included in the request")
        }

        val deviceId = deviceIdParam.first()
        val deviceSpec = deviceManager.getDeviceSpec(deviceId)
                ?: throw HttpException(HttpStatus.NOT_FOUND_404, "No device with id $deviceId found")

        val compressedSplitsResult = bundleManager.generateCompressedSplits(
                applicationId = applicationIdParam.first(),
                version = version,
                variant = variantParam.first(),
                deviceSpec = deviceSpec,
                features = featuresToInstallParam.flatMap { feature -> feature.split(",") }.toTypedArray(),
                languages = languagesToInstallParam.flatMap { feature -> feature.split(",") }.toTypedArray()
        )

        val compressedSplits = when (compressedSplitsResult) {
            is BundleManager.Result.Success -> compressedSplitsResult.path
            else -> throw HttpException(HttpStatus.BAD_REQUEST_400, compressedSplitsResult.message)
        }

        val fileSize = compressedSplits.toFile().length().toInt()
        var byteOffset = 0
        val throttleBy = throttle?.firstOrNull()?.toLongOrNull() ?: 0
        val interval = (throttleBy / 30f).toLong()
        val byteInterval = (fileSize / 30f).toInt()
        val featuresString = featuresToInstallParam.joinToString(",")
        val languagesString = languagesToInstallParam.joinToString(",")
        val buffer = ByteArray(byteInterval)
        logger.i("Sending splits from features [$featuresString] and languages [$languagesString], total size: $fileSize")
        response?.apply {
            contentType = "application/zip"
            setHeader("Content-Disposition", "attachment; filename=splits.zip")
            setContentLength(fileSize)
            compressedSplits.toFile().inputStream().use { inputStream ->
                while (byteOffset < fileSize) {
                    val bytesLeftToWrite = fileSize - byteOffset
                    val writeLength = if (byteOffset + byteInterval > fileSize) {
                        bytesLeftToWrite
                    } else {
                        byteInterval
                    }
                    inputStream.read(buffer, 0, writeLength)
                    outputStream.write(buffer, 0, writeLength)
                    byteOffset += writeLength
                    val percentageSent = Math.round((byteOffset / fileSize.toFloat()) * 100)
                    logger.i("Sent $byteOffset / ${fileSize} ($percentageSent%)")
                    if (interval > 0) {
                        Thread.sleep(interval)
                    }
                }
            }
        }
        logger.i("Finished sending [$featuresString] and [$languagesString]")
    }
}

internal class RegisterDevicePathHandler(
        private val deviceManager: DeviceManager,
        private val gson: Gson,
        private val logger: Logger
) : PathHandler {
    override val path: String = "register"

    override fun handle(request: HttpServletRequest?, response: HttpServletResponse?) {
        if (request?.method != HttpMethod.POST.asString()) {
            throw HttpException(HttpStatus.BAD_REQUEST_400, "method != POST")
        }

        val body = request?.inputStream?.use { it.readString() }

        logger.i("Request body: $body")

        val deviceSpecDto = try {
            gson.fromJson(body, DeviceSpecDto::class.java)
        } catch (exception: Exception) {
            throw HttpException(HttpStatus.BAD_REQUEST_400,
                    "Invalid device spec format")
        }

        val deviceId = deviceManager.registerDevice(deviceSpecDto)

        logger.i("Response body: $deviceId")

        val deviceIdBytes = deviceId.toByteArray()

        response?.apply {
            contentType = "text/plain; charset=utf8"
            setContentLength(deviceIdBytes.size)
            outputStream?.write(deviceIdBytes)
        }
    }
}

internal class UploadBundlePathHandler(
        private val bundleManager: BundleManager,
        private val logger: Logger
) : PathHandler {
    override val path: String = "upload"

    override fun handle(request: HttpServletRequest?, response: HttpServletResponse?) {
        if (request?.contentType?.contains(CONTENT_TYPE_MULTIPART_FORM_DATA) != true) {
            throw HttpException(HttpStatus.BAD_REQUEST_400, "Content-Type != $CONTENT_TYPE_MULTIPART_FORM_DATA")
        }

        val limit = 100 * Math.pow(10.0, 9.0).toLong()
        val threshold = 200 * Math.pow(10.0, 6.0).toInt()
        val multipartConfigElement = MultipartConfigElement("", limit, limit, threshold)
        request.setAttribute(Request.MULTIPART_CONFIG_ELEMENT, multipartConfigElement)

        val bundlePart = request.requirePart("bundle")
        val versionPart = request.requirePart("version")
        val applicationIdPart = request.requirePart("application-id")
        val variantPart = request.requirePart("variant")
        val signingConfigPart = request.requirePart("signing-config")
        val keystorePart = request.requirePart("keystore")

        val applicationId = applicationIdPart.inputStream.use { it.readString() }
        val variant = variantPart.inputStream.use { it.readString() }
        val versionString = versionPart.inputStream.use { it.readString() }
        val signingConfig = signingConfigPart.inputStream.use { it.readString() }
        val version = try {
            versionString.toInt()
        } catch (exception: Exception) {
            throw HttpException(HttpStatus.BAD_REQUEST_400, "Expected version to be an integer, " +
                    "got $versionString")
        }

        val result = bundleManager.storeBundle(
                applicationId = applicationId,
                version = version,
                variant = variant,
                signingConfig = signingConfig,
                bundleInputStream = bundlePart.inputStream,
                keyStoreInputStream = keystorePart.inputStream
        )

        when (result) {
            is BundleManager.Result.Error -> {
                logger.e(result.message)
                response?.sendError(HttpStatus.BAD_REQUEST_400, result.message)
            }
        }
    }

    companion object {
        internal const val CONTENT_TYPE_MULTIPART_FORM_DATA = "multipart/form-data"
    }
}

/**
 * Used for AppEngine health checks
 */
class LivenessPathHandler : PathHandler {
    override val path: String = "liveness_check"
    override val authRequired: Boolean = false

    override fun handle(request: HttpServletRequest?, response: HttpServletResponse?) {
        response?.status = HttpStatus.OK_200
    }
}