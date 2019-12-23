package com.jeppeman.globallydynamic.idea.extensions

import com.intellij.ui.ColorUtil
import java.awt.Color

fun Color.toHex(): String = ColorUtil.toHex(this)