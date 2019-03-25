package se.ansman.kotshi

@JsonSerializable
data class MyClass(
    val name: String = "",
    val address: String = "N/A",
    val age: Int = -1
)