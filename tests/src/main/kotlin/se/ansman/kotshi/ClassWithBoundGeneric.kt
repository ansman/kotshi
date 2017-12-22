package se.ansman.kotshi

@JsonSerializable
data class ClassWithBoundGeneric<out Data>(val data: List<Data>)