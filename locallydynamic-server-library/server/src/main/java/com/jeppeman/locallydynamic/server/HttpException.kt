package com.jeppeman.locallydynamic.server

class HttpException(val code: Int, override val message: String) : RuntimeException(message)