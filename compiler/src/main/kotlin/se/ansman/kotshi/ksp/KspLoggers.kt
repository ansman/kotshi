package se.ansman.kotshi.ksp

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSNode

fun KSPLogger.logKotshiError(error: KspProcessingError) {
    logKotshiError(error.message, error.node)
}

fun KSPLogger.logKotshiError(message: String, node: KSNode?) {
    error("Kotshi: $message", node)
}

fun KSPLogger.logKotshiWarning(message: String, node: KSNode?) {
    warn("Kotshi: $message", node)
}