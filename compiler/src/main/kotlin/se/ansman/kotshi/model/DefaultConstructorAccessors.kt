package se.ansman.kotshi.model

import org.objectweb.asm.Type

data class DefaultConstructorAccessor(
    val targetType: String,
    val parameters: List<Parameter>,
) {
    val accessorName = targetType.replace(REPLACEMENT, "_")

    data class Parameter(
        val type: String,
        val name: String,
        val nullability: Nullability,
    ) {
        enum class Nullability {
            NOT_NULL,
            NULLABLE,
            PLATFORM
        }
    }
}

fun DefaultConstructorAccessor.delegateDescriptor(): String = functionDescriptor("L$targetType;")
fun DefaultConstructorAccessor.targetTypeConstructorDescriptor(): String = functionDescriptor("V") {
    append("Lkotlin/jvm/internal/DefaultConstructorMarker;")
}

private fun DefaultConstructorAccessor.functionDescriptor(
    returnType: String,
    extraParameters: StringBuilder.() -> Unit = {},
): String = buildString {
    append('(')
    for (parameter in parameters) {
        append(parameter.type)
    }
    repeat(maskCount) {
        append(Type.INT_TYPE.descriptor)
    }
    extraParameters()
    append(')')
    append(returnType)
}

val DefaultConstructorAccessor.maskCount: Int
    get() = (parameters.size + 31) / 32

private val REPLACEMENT = Regex("[/$]")