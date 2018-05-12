package se.ansman.kotshi

@JsonSerializable
data class ClassWithJavaKeyword(
    val default: Boolean,
    val int: Int,
    val case: Int
)