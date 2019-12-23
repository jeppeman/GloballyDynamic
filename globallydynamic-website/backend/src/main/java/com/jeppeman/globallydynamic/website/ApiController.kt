package com.jeppeman.globallydynamic.website

import org.apache.http.client.methods.RequestBuilder
import org.apache.http.entity.ContentType
import org.apache.http.entity.InputStreamEntity
import org.apache.http.entity.mime.FormBodyPartBuilder
import org.apache.http.entity.mime.HttpMultipartMode
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.InputStreamBody
import org.apache.http.impl.client.HttpClients
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import java.net.URL
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


@Controller
class ApiController {
    @RequestMapping(
        method = [RequestMethod.GET, RequestMethod.POST],
        value = ["/api/**"]
    )
    fun forwardToGloballyDynamicServer(request: HttpServletRequest, response: HttpServletResponse) {
        try {
            val path = request.requestURI.split("/api")[1]
            val url = URL(
                "http://localhost:9090"
                        + path
                        + if (request.queryString != null) "?" + request.queryString else ""
            )
            val headers = request.headerNames
            val requestBuilder = if (request.method == "POST") {
                RequestBuilder.post(url.toURI())
            } else {
                RequestBuilder.get(url.toURI())
            }
            while (headers.hasMoreElements()) {
                val header = headers.nextElement()
                val values = request.getHeaders(header)
                while (values.hasMoreElements()) {
                    val value = values.nextElement()
                    requestBuilder.addHeader(header, value)
                }
            }
            if (request.contentType?.contains("multipart/form-data") == true) {
                requestBuilder.removeHeaders("Content-Length")
                requestBuilder.removeHeaders("Content-Type")
                val multiPartBuilder = MultipartEntityBuilder.create()
                    .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                request.parts.forEach { part ->
                    multiPartBuilder.addPart(
                        FormBodyPartBuilder.create()
                            .setBody(
                                InputStreamBody(
                                    part.inputStream,
                                    ContentType.create(part.contentType ?: "text/plain")
                                )
                            )
                            .setName(part.name)
                            .build()
                    )
                }
                requestBuilder.entity = multiPartBuilder.build()
            } else {
                requestBuilder.removeHeaders("Content-Type")
                requestBuilder.removeHeaders("Content-Length")
                requestBuilder.entity = InputStreamEntity(request.inputStream, ContentType.getByMimeType(request.contentType))
            }
            val globallyDynamicResponse = HttpClients.createDefault().execute(requestBuilder.build())
            response.status = globallyDynamicResponse.statusLine.statusCode
            globallyDynamicResponse.allHeaders.forEach { header ->
                response.setHeader(header.name, header.value)
            }
            globallyDynamicResponse.entity.content.copyTo(response.outputStream)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}