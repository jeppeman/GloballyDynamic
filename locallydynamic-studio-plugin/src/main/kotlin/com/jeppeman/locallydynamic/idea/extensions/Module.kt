package com.jeppeman.locallydynamic.idea.extensions

import com.android.tools.idea.gradle.util.GradleUtil
import com.intellij.openapi.module.Module
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.GrListOrMap
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrLiteral
import java.util.HashMap

val Module.hasLocallyDynamicEnabled: Boolean
    get() {
        GradleUtil.getGradleBuildFile(this)?.let { file ->
            file.toPsiFile(project)?.let { psiFile ->
                return when (psiFile) {
                    !is GroovyFile -> false
                    else -> psiFile.plugins.contains("com.jeppeman.locallydynamic")
                }
            }
        }
        return false
    }

val GroovyFile.plugins: List<String>
    get() = ArrayList<String>().apply {
        statements.toList().filterIsInstance<GrMethodCall>()
            .filter { "apply" == it.invokedExpression.text }
            .forEach { methodCall ->
                val values = HashMap<String?, Any?>()
                methodCall.argumentList.allArguments.filterIsInstance<GrNamedArgument>().forEach {
                    values[it.labelName] = it.expression?.parseValueExpression()
                }
                values["plugin"]?.let { plugin ->
                    add(plugin.toString())
                }
            }
    }

private fun GrExpression.parseValueExpression(): Any? {
    return when (this) {
        is GrLiteral -> value
        is GrListOrMap -> {
            if (isMap) return null
            ArrayList<Any>().apply {
                initializers.forEach { subexpression ->
                    subexpression.parseValueExpression()?.let { subValue ->
                        add(subValue)
                    }
                }
            }
        }
        else -> null
    }
}
