package com.jeppeman.locallydynamic.idea

import com.intellij.diagnostic.logging.DefaultLogFilterModel
import com.intellij.diagnostic.logging.DefaultLogFormatter
import com.intellij.diagnostic.logging.LogConsoleBase
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class LocallyDynamicConsole(
    project: Project,
    locallyDynamicLogFormatter: LocallyDynamicLogFormatter,
    locallyDynamicLogFilterModel: LocallyDynamicLogFilterModel,
    private val locallyDynamicServerManager: LocallyDynamicServerManager
) : LogConsoleBase(
    project,
    null,
    "",
    false,
    locallyDynamicLogFilterModel,
    GlobalSearchScope.allScope(project),
    locallyDynamicLogFormatter
) {

    init {
        locallyDynamicServerManager.logger.content.forEach(::addMessage)
        locallyDynamicServerManager.logger.registerUpdateListener(::addMessage)
    }

    override fun isActive(): Boolean = locallyDynamicServerManager.isRunning
}

class LocallyDynamicLogFormatter : DefaultLogFormatter() {
    private val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern(
        "yyyy-MM-dd HH:mm:ss",
        Locale.getDefault()
    )

    private fun String.timestamped(): String {
        return "${LocalDateTime.now().format(dateTimeFormatter)}: $this"
    }

    override fun formatMessage(msg: String?): String {
        val slashIndex = msg?.indexOfFirst { c -> c == '/' } ?: 0
        return msg?.substring(slashIndex + 1)
            ?.timestamped()
            ?: super.formatMessage(msg)
    }
}

class LocallyDynamicLogFilterModel(project: Project) : DefaultLogFilterModel(project) {
    override fun processLine(line: String?): MyProcessingResult {
        return if (line != null) {
            val slashIndex = line.indexOfFirst { c -> c == '/' }
            val processOutputType = when(line.substring(0, slashIndex)) {
                ProcessOutputTypes.STDERR.toString() -> ProcessOutputTypes.STDERR
                ProcessOutputTypes.SYSTEM.toString() -> ProcessOutputTypes.SYSTEM
                else -> ProcessOutputTypes.STDOUT
            }
            MyProcessingResult(processOutputType, true, null)
        } else {
            super.processLine(line)
        }
    }
}