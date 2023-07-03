package com.jeppeman.globallydynamic.server

import com.jeppeman.globallydynamic.server.extensions.stackTraceToString
import com.jeppeman.globallydynamic.server.extensions.toBase64
import com.jeppeman.globallydynamic.server.server.BuildConfig
import org.eclipse.jetty.http.HttpStatus
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

private val HttpServletRequest.requestLine get() = "$method $requestURL${queryString?.let { "?$it" } ?: ""}"

private val HttpServletRequest.isSsl
    get() = scheme == "https"
        || getHeader("X-Forwarded-Proto") == "https"

internal class RequestHandler(
    internal val configuration: GloballyDynamicServer.Configuration,
    internal val pathHandlers: List<PathHandler>
) : AbstractHandler() {
    private val logger = configuration.logger

    private fun PathHandler?.i(message: String) {
        if (this == null || this.loggingEnabled) {
            logger.i(message)
        }
    }

    private fun PathHandler?.e(message: String) {
        if (this == null || this.loggingEnabled) {
            logger.e(message)
        }
    }

    private fun PathHandler?.e(throwable: Throwable) {
        if (this == null || this.loggingEnabled) {
            logger.e(throwable)
        }
    }

    override fun handle(
        target: String?,
        baseRequest: Request?,
        request: HttpServletRequest?,
        response: HttpServletResponse?
    ) {
        var pathHandler: PathHandler? = null
        try {
            val firstSegment = request?.pathInfo?.split("/")?.firstOrNull(String::isNotBlank)

            pathHandler = firstSegment?.let {
                pathHandlers.firstOrNull { handler -> handler.path.startsWith(firstSegment) }
            }

            pathHandler.i("<-- ${request?.requestLine}")
            pathHandler.i("Request-IP: ${request?.remoteAddr}")
            request?.headerNames?.asSequence()?.forEach { name ->
                pathHandler.i("$name: ${request.getHeader(name)}")
            }


            if (request?.isSsl != true && configuration.httpsRedirect) {
                val redirectUrl = "${request?.requestURL}${request?.queryString?.let { "?$it" } ?: ""}"
                    .replace("http://", "https://")
                response?.sendRedirect(redirectUrl)
            } else {
                if (pathHandler == null) {
                    throw HttpException(HttpStatus.NOT_FOUND_404, "No handler for path ${request?.pathInfo} found")
                }

                if (pathHandler.authRequired
                    && configuration.username.isNotBlank()
                    && configuration.password.isNotBlank()) {
                    val authHeader = request?.getHeader("Authorization")
                    val maybeEncodedCredentials = authHeader?.split("Basic ")?.takeIf { it.size > 1 }?.get(1)
                    val encodedCredentials = "${configuration.username}:${configuration.password}".toBase64()

                    if (maybeEncodedCredentials != encodedCredentials) {
                        throw HttpException(HttpStatus.UNAUTHORIZED_401, "Invalid username or password")
                    }
                }

                pathHandler.handle(request, response)
            }
        } catch (httpException: HttpException) {
            pathHandler.e(httpException.message)
            response?.transmitError(httpException.code, httpException.message)
        } catch (throwable: Throwable) {
            pathHandler.e(throwable)
            response?.transmitError(HttpStatus.INTERNAL_SERVER_ERROR_500, throwable.stackTraceToString())
        } finally {
            baseRequest?.isHandled = true
            pathHandler.i("--> ${response?.status} ${request?.requestLine}")
        }
    }
}

private fun HttpServletResponse.transmitError(code: Int, message: String) {
    status = code
    contentType = "application/json; charset=utf8"
    val errorBody = """{ "error": { "code": $code, "message": "$message" }, "server": "GloballyDynamic Server v${BuildConfig.VERSION}" }"""
    outputStream.write(errorBody.toByteArray())
}
