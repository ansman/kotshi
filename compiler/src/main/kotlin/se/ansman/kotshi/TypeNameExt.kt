package se.ansman.kotshi

import com.squareup.javapoet.TypeName

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