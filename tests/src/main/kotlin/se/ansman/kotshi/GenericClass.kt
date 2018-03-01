package se.ansman.kotshi

@JsonSerializable
data class GenericClass<out T : CharSequence, out C : Collection<T>>(val collection: C, val value: T)
