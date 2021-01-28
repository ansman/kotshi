package se.ansman.kotshi

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeVariableName

data class GeneratedAdapter(
    val targetType: ClassName,
    val className: ClassName,
    val typeVariables: List<TypeVariableName>,
    val requiresTypes: Boolean,
    val requiresMoshi: Boolean
)
