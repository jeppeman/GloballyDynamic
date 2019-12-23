package com.jeppeman.globallydynamic.server

import javax.servlet.ReadListener
import javax.servlet.ServletInputStream

class FakeInputStream(private val body: String) : ServletInputStream() {
    private var currentIndex = 0
    override fun isReady() = currentIndex < body.length
    override fun isFinished() = currentIndex >= body.length
    override fun setReadListener(readListener: ReadListener?) = Unit

    override fun read(): Int = if (currentIndex < this.body.length) {
        this.body[currentIndex++].toInt()
    } else {
        -1
    }
}