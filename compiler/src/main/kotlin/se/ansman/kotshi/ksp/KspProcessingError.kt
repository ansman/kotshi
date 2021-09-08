package se.ansman.kotshi.ksp

import com.google.devtools.ksp.symbol.KSNode

class KspProcessingError(override val message: String, val node: KSNode) : Exception(message)