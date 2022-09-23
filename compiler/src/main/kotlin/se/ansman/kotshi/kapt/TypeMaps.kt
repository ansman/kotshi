package se.ansman.kotshi.kapt

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import se.ansman.kotshi.Types

fun TypeName.toKotlinVersion(mutable: Boolean = true): TypeName =
    when (this) {
        is ClassName -> toKotlinVersion(mutable)
        Dynamic -> this
        is LambdaTypeName -> this
        is ParameterizedTypeName -> rawType.toKotlinVersion(mutable)
            .parameterizedBy(typeArguments.map { it.toKotlinVersion(mutable) })
        is TypeVariableName -> copy(bounds = bounds.map { it.toKotlinVersion(mutable) })
        is WildcardTypeName -> when {
            inTypes.size == 1 -> WildcardTypeName.consumerOf(inTypes[0].toKotlinVersion(mutable))
            outTypes == STAR.outTypes -> STAR
            else -> WildcardTypeName.producerOf(outTypes[0].toKotlinVersion(mutable))
        }
    }

fun ClassName.toKotlinVersion(mutable: Boolean = true): ClassName =
    when (this) {
        JavaTypes.boolean -> BOOLEAN
        JavaTypes.byte -> BYTE
        JavaTypes.char -> CHAR
        JavaTypes.short -> SHORT
        JavaTypes.int -> INT
        JavaTypes.long -> LONG
        JavaTypes.float -> FLOAT
        JavaTypes.double -> DOUBLE
        JavaTypes.string -> STRING
        JavaTypes.annotation -> Types.Kotlin.annotation
        JavaTypes.collection -> if (mutable) MUTABLE_COLLECTION else COLLECTION
        JavaTypes.list -> if (mutable) MUTABLE_LIST else LIST
        JavaTypes.set -> if (mutable) MUTABLE_SET else SET
        JavaTypes.map -> if (mutable) MUTABLE_MAP else MAP
        else -> this
    }

private object JavaTypes {
    val boolean = ClassName("java.lang", "Boolean")
    val byte = ClassName("java.lang", "Byte")
    val char = ClassName("java.lang", "Char")
    val short = ClassName("java.lang", "Short")
    val int = ClassName("java.lang", "Int")
    val long = ClassName("java.lang", "Long")
    val float = ClassName("java.lang", "Float")
    val double = ClassName("java.lang", "Double")
    val string = ClassName("java.lang", "String")
    val annotation = ClassName("java.lang.annotation", "Annotation")
    val collection = ClassName("java.util", "Collection")
    val list = ClassName("java.util", "List")
    val set = ClassName("java.util", "Set")
    val map = ClassName("java.util", "Map")
}