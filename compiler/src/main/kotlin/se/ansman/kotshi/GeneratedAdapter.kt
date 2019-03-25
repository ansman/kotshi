package se.ansman.kotshi

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeVariableName

data class GeneratedAdapter(
    val targetType: ClassName,
    val className: ClassName,
    val typeVariables: List<TypeVariableName>,
    val requiresMoshi: Boolean = true
) {
    val requiresTypes: Boolean = typeVariables.isNotEmpty()
    init {
        assert(!requiresTypes || requiresMoshi) {
            "An adapter requiring types must also require a Moshi instance."
        }
    }
}
