package se.ansman.kotshi

@JsonSerializable(useAdaptersForPrimitives = PrimitiveAdapters.ENABLED)
data class ClassWithWeirdNames(
    val OPTIONS: Int,
    val writer: Boolean,
    val value: String,
    val reader: Char,
    val adapter1: Byte,
    val adapter2: Int?,
    val types: List<String>,
    val moshi: String
)
