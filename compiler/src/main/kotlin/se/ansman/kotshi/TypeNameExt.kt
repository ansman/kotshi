package se.ansman.kotshi

import com.squareup.javapoet.ArrayTypeName
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeVariableName
import com.squareup.javapoet.WildcardTypeName

val TYPE_NAME_STRING: TypeName = TypeName.get(String::class.java)

val TypeName.jvmDefault: String
    get() {
        if (!isPrimitive) return "null"
        return when (this) {
            TypeName.BOOLEAN -> "false"
            TypeName.BYTE -> "0"
            TypeName.SHORT -> "0"
            TypeName.INT -> "0"
            TypeName.LONG -> "0"
            TypeName.CHAR -> "0"
            TypeName.FLOAT -> "0f"
            TypeName.DOUBLE -> "0.0"
            else -> throw AssertionError("Unknown type $this")
        }
    }

fun TypeName.isGeneric(recursive: Boolean = true): Boolean = when (this) {
    is ArrayTypeName -> componentType.isGeneric()
    is ParameterizedTypeName -> recursive && typeArguments.any { it.isGeneric(true) }
    is WildcardTypeName -> recursive && upperBounds.any { it.isGeneric(true) } || lowerBounds.any { it.isGeneric(true) }
    is TypeVariableName -> true
    is ClassName -> false
    else -> false
}

val TypeName.rawType: TypeName
    get() = when (this) {
        is ParameterizedTypeName -> this.rawType
        else -> this
    }