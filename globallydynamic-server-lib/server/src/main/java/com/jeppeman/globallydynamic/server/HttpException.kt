package com.jeppeman.globallydynamic.server

class HttpException(val code: Int, override val message: String) : RuntimeException(message)