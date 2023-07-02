package com.jeppeman.globallydynamic.idea.utils

import com.intellij.ui.ColorUtil
import java.awt.Color

fun Color.toHex(): String = ColorUtil.toHex(this)