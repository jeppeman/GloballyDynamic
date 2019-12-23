package com.example.locallydynamic

import android.content.Context
import java.util.*
import kotlin.reflect.KClass

interface Language {
    val displayName: String
    val code: String
    val locale: Locale

    companion object {
        fun create(context: Context, type: KClass<out Language>): Language {
            return when (type) {
                Swedish::class -> Swedish(context)
                Korean::class -> Korean(context)
                German::class -> German(context)
                Italian::class -> Italian(context)
                else -> throw IllegalArgumentException("Language not supported $type")
            }
        }
    }
}

class Swedish(context: Context) : Language {
    override val displayName: String = context.getString(R.string.swedish)
    override val code: String = "sv"
    override val locale: Locale = Locale("sv", "se")
}

class Korean(context: Context) : Language {
    override val displayName: String = context.getString(R.string.korean)
    override val code: String = "ko"
    override val locale: Locale = Locale("ko", "kr")
}

class German(context: Context) : Language {
    override val displayName: String = context.getString(R.string.german)
    override val code: String = "de"
    override val locale: Locale = Locale("de", "de")
}

class Italian(context: Context) : Language {
    override val displayName: String = context.getString(R.string.italian)
    override val code: String = "it"
    override val locale: Locale = Locale("it", "it")
}