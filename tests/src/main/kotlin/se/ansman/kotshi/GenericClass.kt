package se.ansman.kotshi

@Target(AnnotationTarget.TYPE)
annotation class SomeTypeAnnotation

@JsonSerializable
data class GenericClass<out T : CharSequence, out C : Collection<T>>(
    val collection: C,
    val value: T,
    val valueWithTypeAnnotation: @SomeTypeAnnotation T
)
