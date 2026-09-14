package com.sounddeck.core.log

import com.sounddeck.core.model.DiagnosticLog
import com.sounddeck.core.model.LogLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentLinkedDeque

object DiagnosticLogManager {
    private const val MAX_LOGS = 250
    private val logsQueue = ConcurrentLinkedDeque<DiagnosticLog>()

    private val _logsFlow = MutableStateFlow<List<DiagnosticLog>>(emptyList())
    val logsFlow: StateFlow<List<DiagnosticLog>> = _logsFlow.asStateFlow()

    fun log(level: LogLevel, tag: String, message: String) {
        val entry = DiagnosticLog(
            level = level,
            tag = tag,
            message = message
        )
        logsQueue.addFirst(entry)
        while (logsQueue.size > MAX_LOGS) {
            logsQueue.removeLast()
        }
        _logsFlow.value = logsQueue.toList()
    }

    fun d(tag: String, message: String) = log(LogLevel.DEBUG, tag, message)
    fun i(tag: String, message: String) = log(LogLevel.INFO, tag, message)
    fun w(tag: String, message: String) = log(LogLevel.WARN, tag, message)
    fun e(tag: String, message: String) = log(LogLevel.ERROR, tag, message)

    fun clear() {
        logsQueue.clear()
        _logsFlow.value = emptyList()
    }
}
