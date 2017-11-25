package se.ansman.kotshi

@JsonDefaultValue
annotation class StringWithNA

@JsonSerializable
data class MyClass(
        @JsonDefaultValue
        val name: String,
        @StringWithNA
        val address: String
) {
    companion object {
        @JsonDefaultValue
        @JvmField
        val defaultString = ""

        @StringWithNA
        fun defaultStringWithNA() = "N/A"
    }
}