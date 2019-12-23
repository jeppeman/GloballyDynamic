package com.jeppeman.locallydynamic.gradle

import com.android.build.gradle.api.ApplicationVariant
import org.gradle.api.Action
import org.gradle.api.Task

abstract class VariantTaskAction<T : Task>(protected val applicationVariant: ApplicationVariant) : Action<T> {
    abstract val name: String
}