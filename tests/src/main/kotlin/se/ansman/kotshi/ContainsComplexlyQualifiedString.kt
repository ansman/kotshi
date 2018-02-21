package se.ansman.kotshi

import com.squareup.moshi.JsonQualifier
import kotlin.reflect.KClass

// TODO: Elements.getElementValuesWithDefaults(AnnotationMirror) does not find elements with Java keyword names
// TODO: like `class`, boolean, switch, enum!

@JsonQualifier
annotation class WithStringElement(val string: String)

@JsonQualifier
annotation class WithNumberElement(val number: Int)

@JsonQualifier
annotation class WithBooleanElement(val bool: Boolean)

@JsonQualifier
annotation class WithClassElement(val cls: KClass<*>)

@JsonQualifier
annotation class WithEnumElement(val someEnum: SomeEnum)

@JsonQualifier
annotation class WithArrayElements(
        val stringArray: Array<String>,
        val byteArray: ByteArray,
        val classArray: Array<KClass<*>>
)

@JsonQualifier
annotation class WithDefaultStringElement(val string: String = "default")

@JsonSerializable
data class ContainsComplexlyQualifiedString(
        @WithStringElement("\\\$Hello, ")
        @WithNumberElement(4)
        @WithBooleanElement(true)
        @WithClassElement(ContainsComplexlyQualifiedString::class)
        @WithEnumElement(SomeEnum.VALUE5)
        @WithArrayElements(["one", "", "three"], [5], [])
        @WithDefaultStringElement
        val string: String
)
