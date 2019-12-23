package com.jeppeman.globallydynamic.website

import org.springframework.boot.web.servlet.error.ErrorController
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping

private const val ERROR = "/error"

@Controller
class ErrorController : ErrorController {
    @RequestMapping(value = [ERROR])
    fun error(): String = "forward:/index.html"

    override fun getErrorPath(): String = ERROR
}