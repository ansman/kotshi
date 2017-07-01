package se.ansman.kotshi

@JsonSerializable
data class GenericClassWithQualifier<out T>(@Hello val value: T)
