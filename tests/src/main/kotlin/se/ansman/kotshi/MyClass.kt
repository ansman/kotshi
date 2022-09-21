package se.ansman.kotshi

@JsonSerializable
data class MyClass(
    val name: String = "",
    val address: String = "N/A",
    val age: Int = -1
) {
    companion object {
        @JvmStatic
        fun create(
            name: String,
            address: String,
            age: Int
        ): MyClass = MyClass(name, address, age)
    }
}