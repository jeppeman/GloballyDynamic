package com.example.locallydynamic

import android.animation.ObjectAnimator
import android.view.animation.AccelerateInterpolator
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.ProgressBar
import android.widget.TextView
import kotlin.math.roundToInt

fun ProgressBar.animateBetween(from: Int, to: Int, duration: Long) {
    val animator = ObjectAnimator.ofInt(this, "progress", from, to)
    animator.interpolator = AccelerateInterpolator()
    animator.duration = duration
    animator.start()
}

fun TextView.animateProgress(from: Int, to: Int, duration: Long) {
    val animation = object : Animation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
            val total = to - from
            val current = from + total * interpolatedTime
            text = "${current.roundToInt()}%"
        }
    }

    this.clearAnimation()
    this.animation = animation
    animation.duration = duration
    animation.start()
}