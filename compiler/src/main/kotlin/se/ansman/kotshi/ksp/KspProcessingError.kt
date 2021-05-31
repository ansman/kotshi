package se.ansman.kotshi.ksp

import com.google.devtools.ksp.symbol.KSNode

class KspProcessingError(message: String, val node: KSNode) : Exception(message)