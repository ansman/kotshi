package se.ansman.kotshi

@JsonSerializable
data class ClassWithLambda(val block: (String) -> Boolean)