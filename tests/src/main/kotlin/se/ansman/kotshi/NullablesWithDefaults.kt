package se.ansman.kotshi

@JsonSerializable
data class NullablesWithDefaults(
    val v1: Byte? = Byte.MAX_VALUE,
    val v2: Char? = Char.MAX_VALUE,
    val v3: Short? = Short.MAX_VALUE,
    val v4: Int? = Int.MAX_VALUE,
    val v5: Long? = Long.MAX_VALUE,
    val v6: Float? = Float.MAX_VALUE,
    val v7: Double? = Double.MAX_VALUE,
    val v8: String? = "n/a",
    val v9: List<String>? = emptyList()
)