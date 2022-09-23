package se.ansman.kotshi.model

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import se.ansman.kotshi.*

data class AnnotationModel(
    val annotationName: ClassName,
    val hasMethods: Boolean,
    val values: Map<String, Value<*>>
) {
    sealed class Value<T : Any> {
        abstract val value: T

        sealed class Single<T : Any> : Value<T>()
        sealed class Primitive<T : Any> : Single<T>()
        sealed class Object<T : Any> : Single<T>()

        data class Class(override val value: ClassName) : Object<ClassName>()
        data class Annotation(override val value: AnnotationModel) : Object<AnnotationModel>()
        data class Enum(val enumType: ClassName, override val value: kotlin.String) : Object<kotlin.String>()
        data class String(override val value: kotlin.String) : Object<kotlin.String>()

        data class Float(override val value: kotlin.Float) : Primitive<kotlin.Float>()
        data class Char(override val value: kotlin.Char) : Primitive<kotlin.Char>()
        data class Boolean(override val value: kotlin.Boolean) : Primitive<kotlin.Boolean>()
        data class Double(override val value: kotlin.Double) : Primitive<kotlin.Double>()
        data class Byte(override val value: kotlin.Byte) : Primitive<kotlin.Byte>()
        data class UByte(override val value: kotlin.UByte) : Primitive<kotlin.UByte>()
        data class Short(override val value: kotlin.Short) : Primitive<kotlin.Short>()
        data class UShort(override val value: kotlin.UShort) : Primitive<kotlin.UShort>()
        data class Int(override val value: kotlin.Int) : Primitive<kotlin.Int>()
        data class UInt(override val value: kotlin.UInt) : Primitive<kotlin.UInt>()
        data class Long(override val value: kotlin.Long) : Primitive<kotlin.Long>()
        data class ULong(override val value: kotlin.ULong) : Primitive<kotlin.ULong>()

        sealed class Array<A : Single<*>> : Value<List<A>>() {
            data class Object(val elementType: TypeName, override val value: List<Value.Object<*>>) : Array<Value.Object<*>>()
            data class Float(override val value: List<Value.Float>) : Array<Value.Float>()
            data class Char(override val value: List<Value.Char>) : Array<Value.Char>()
            data class Boolean(override val value: List<Value.Boolean>) : Array<Value.Boolean>()
            data class Double(override val value: List<Value.Double>) : Array<Value.Double>()
            data class Byte(override val value: List<Value.Byte>) : Array<Value.Byte>()
            data class UByte(override val value: List<Value.UByte>) : Array<Value.UByte>()
            data class Short(override val value: List<Value.Short>) : Array<Value.Short>()
            data class UShort(override val value: List<Value.UShort>) : Array<Value.UShort>()
            data class Int(override val value: List<Value.Int>) : Array<Value.Int>()
            data class UInt(override val value: List<Value.UInt>) : Array<Value.UInt>()
            data class Long(override val value: List<Value.Long>) : Array<Value.Long>()
            data class ULong(override val value: List<Value.ULong>) : Array<Value.ULong>()
        }
    }
}

fun AnnotationModel.render(createAnnotationsUsingConstructor: Boolean): CodeBlock =
    if (createAnnotationsUsingConstructor) {
        CodeBlock.builder()
            .add("%T(", annotationName)
            .indent()
            .applyEach(values.entries) { (name, value) ->
                add("\n%N·=·%L,", name, value.render(true))
            }
            .unindent()
            .applyIf(values.isNotEmpty()) { add("\n") }
            .add(")")
            .build()
    } else if (values.isEmpty()) {
        CodeBlock.of("%T::class.java.%M()", annotationName, Functions.Kotshi.createJsonQualifierImplementation)
    } else {
        CodeBlock.builder()
            .add("%T::class.java.%M(mapOf(", annotationName, Functions.Kotshi.createJsonQualifierImplementation)
            .withIndent {
                values.entries.forEachIndexed { i, (name, value) ->
                    if (i > 0) {
                        add(",")
                    }
                    add("\n%S·to·%L", name, value.render(false))
                }
            }
            .add("\n))")
            .build()
    }

private fun AnnotationModel.Value<*>.render(createAnnotationsUsingConstructor: Boolean): CodeBlock =
    when (this) {
        is AnnotationModel.Value.Boolean ->
            CodeBlock.of("%L", value)
        is AnnotationModel.Value.Byte ->
            CodeBlock.of("(%L).toByte()", value)
        is AnnotationModel.Value.UByte ->
            if (createAnnotationsUsingConstructor) CodeBlock.of("%LU", value) else CodeBlock.of("(%LU).toByte()", value)
        is AnnotationModel.Value.Char ->
            CodeBlock.of("'%L'", if (value == '\'') "\\'" else value)
        is AnnotationModel.Value.Class ->
            if (createAnnotationsUsingConstructor) CodeBlock.of("%T::class", value) else CodeBlock.of("%T::class.java", value)
        is AnnotationModel.Value.Annotation ->
            value.render(createAnnotationsUsingConstructor)
        is AnnotationModel.Value.Double -> {
            var s = value.toString()
            if ('.' !in s) s += ".0"
            CodeBlock.of("%L", s)
        }
        is AnnotationModel.Value.Enum ->
            CodeBlock.of("%T.%L", enumType, value)
        is AnnotationModel.Value.Float ->
            CodeBlock.of("%Lf", value)
        is AnnotationModel.Value.Int ->
            CodeBlock.of("%L", value)
        is AnnotationModel.Value.UInt ->
            if (createAnnotationsUsingConstructor) CodeBlock.of("%LU", value) else CodeBlock.of("(%LU).toInt()", value)
        is AnnotationModel.Value.Long -> {
            if (value == Long.MIN_VALUE) {
                CodeBlock.of("%T.MIN_VALUE", LONG)
            } else {
                CodeBlock.of("%LL", value)
            }
        }
        is AnnotationModel.Value.ULong ->
            if (createAnnotationsUsingConstructor) CodeBlock.of("%LUL", value) else CodeBlock.of("(%LUL).toLong()", value)
        is AnnotationModel.Value.Short ->
            CodeBlock.of("(%L).toShort()", value)
        is AnnotationModel.Value.UShort ->
            if (createAnnotationsUsingConstructor) CodeBlock.of("%LU", value) else CodeBlock.of("(%LU).toShort()", value)
        is AnnotationModel.Value.String ->
            CodeBlock.of("%S", value)
        is AnnotationModel.Value.Array<*> -> {
            CodeBlock.builder()
                .add(
                    when (this) {
                        is AnnotationModel.Value.Array.Boolean ->
                            CodeBlock.of("%M", Functions.Kotlin.booleanArrayOf)
                        is AnnotationModel.Value.Array.Char ->
                            CodeBlock.of("%M", Functions.Kotlin.charArrayOf)
                        is AnnotationModel.Value.Array.Double ->
                            CodeBlock.of("%M", Functions.Kotlin.doubleArrayOf)
                        is AnnotationModel.Value.Array.Float ->
                            CodeBlock.of("%M", Functions.Kotlin.floatArrayOf)
                        is AnnotationModel.Value.Array.Byte ->
                            CodeBlock.of("%M", Functions.Kotlin.byteArrayOf)
                        is AnnotationModel.Value.Array.UByte ->
                            if (createAnnotationsUsingConstructor) {
                                CodeBlock.of("%M", Functions.Kotlin.ubyteArrayOf)
                            } else {
                                CodeBlock.of("%M", Functions.Kotlin.byteArrayOf)
                            }
                        is AnnotationModel.Value.Array.Short ->
                            CodeBlock.of("%M", Functions.Kotlin.shortArrayOf)
                        is AnnotationModel.Value.Array.UShort ->
                            if (createAnnotationsUsingConstructor) {
                                CodeBlock.of("%M", Functions.Kotlin.ushortArrayOf)
                            } else {
                                CodeBlock.of("%M", Functions.Kotlin.shortArrayOf)
                            }
                        is AnnotationModel.Value.Array.Int ->
                            CodeBlock.of("%M", Functions.Kotlin.intArrayOf)
                        is AnnotationModel.Value.Array.UInt ->
                            if (createAnnotationsUsingConstructor) {
                                CodeBlock.of("%M", Functions.Kotlin.uintArrayOf)
                            } else {
                                CodeBlock.of("%M", Functions.Kotlin.intArrayOf)
                            }
                        is AnnotationModel.Value.Array.Long ->
                            CodeBlock.of("%M", Functions.Kotlin.longArrayOf)
                        is AnnotationModel.Value.Array.ULong ->
                            if (createAnnotationsUsingConstructor) {
                                CodeBlock.of("%M", Functions.Kotlin.ulongArrayOf)
                            } else {
                                CodeBlock.of("%M", Functions.Kotlin.longArrayOf)
                            }
                        is AnnotationModel.Value.Array.Object ->
                            if (createAnnotationsUsingConstructor) {
                                CodeBlock.of("%M", Functions.Kotlin.arrayOf)
                            } else {
                                val elementType = elementType.withoutVariance()
                                CodeBlock.of(
                                    "%M<%T>",
                                    Functions.Kotlin.arrayOf,
                                    if (elementType is ParameterizedTypeName && elementType.rawType == Types.Kotlin.kClass) {
                                        Types.Java.clazz.parameterizedBy(elementType.typeArguments.single())
                                    } else {
                                        elementType
                                    }
                                )
                            }
                    }
                )
                .add("(")
                .applyEachIndexed(value) { i, v ->
                    if (i > 0) {
                        add(", ")
                    }
                    add(v.render(createAnnotationsUsingConstructor))
                }
                .add(")")
                .build()
        }
    }