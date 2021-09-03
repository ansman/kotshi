package se.ansman.kotshi.model

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName

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