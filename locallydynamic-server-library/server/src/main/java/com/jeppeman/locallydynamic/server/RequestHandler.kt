package com.jeppeman.locallydynamic.server

import com.jeppeman.locallydynamic.server.extensions.stackTraceToString
import com.jeppeman.locallydynamic.server.extensions.toBase64
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
    internal val username: String,
    internal val password: String,
    internal val httpsRedirect: Boolean,
    internal val pathHandlers: List<PathHandler>,
    internal val logger: Logger
) : AbstractHandler() {

    override fun handle(
        target: String?,
        baseRequest: Request?,
        request: HttpServletRequest?,
        response: HttpServletResponse?
    ) {
        try {
            logger.i("<-- ${request?.requestLine}")
            logger.i("Request-IP: ${request?.remoteAddr}")
            request?.headerNames?.asSequence()?.forEach { name ->
                logger.i("$name: ${request.getHeader(name)}")
            }

            if (request?.isSsl != true && httpsRedirect) {
                val redirectUrl = "${request?.requestURL}${request?.queryString?.let { "?$it" } ?: ""}"
                    .replace("http://", "https://")
                logger.i("Redirecting to $redirectUrl")
                response?.sendRedirect(redirectUrl)
            } else {
                val firstSegment = request?.pathInfo?.split("/")?.firstOrNull(String::isNotBlank)

                val pathHandler = firstSegment?.let {
                    pathHandlers.firstOrNull { handler -> handler.path.startsWith(firstSegment) }
                } ?: throw HttpException(HttpStatus.NOT_FOUND_404, "No handler for path ${request?.pathInfo} found")

                if (pathHandler.authRequired && username.isNotBlank() && password.isNotBlank()) {
                    val authHeader = request.getHeader("Authorization")
                    val maybeEncodedCredentials = authHeader?.split("Basic ")?.takeIf { it.size > 1 }?.get(1)
                    val encodedCredentials = "$username:$password".toBase64()

                    if (maybeEncodedCredentials != encodedCredentials) {
                        throw HttpException(HttpStatus.UNAUTHORIZED_401, "Invalid username or password")
                    }
                }

                pathHandler.handle(request, response)
            }
        } catch (httpException: HttpException) {
            logger.e(httpException.message)
            response?.sendError(httpException.code, httpException.message)
        } catch (throwable: Throwable) {
            logger.e(throwable)
            response?.sendError(
                HttpStatus.INTERNAL_SERVER_ERROR_500,
                throwable.stackTraceToString()
            )
        } finally {
            baseRequest?.isHandled = true
            logger.i("--> ${response?.status} ${request?.requestLine}")
        }
    }
}