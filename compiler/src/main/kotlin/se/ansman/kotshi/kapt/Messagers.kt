package se.ansman.kotshi.kapt

import javax.annotation.processing.Messager
import javax.lang.model.element.Element
import javax.tools.Diagnostic

fun Messager.logKotshiError(error: KaptProcessingError) {
    logKotshiError(error.message, error.element)
}

fun Messager.logKotshiError(message: String, element: Element?) {
    printMessage(Diagnostic.Kind.ERROR, "Kotshi: $message", element)
}

fun Messager.logKotshiWarning(message: String, element: Element?) {
    printMessage(Diagnostic.Kind.WARNING, "Kotshi: $message", element)
}