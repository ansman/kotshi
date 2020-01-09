package se.ansman.kotshi

@JsonSerializable(useAdaptersForPrimitives = PrimitiveAdapters.ENABLED)
data class ClassWithWeirdNames(
    val options: Int,
    val it: Int,
    val writer: Boolean,
    val value: String,
    val reader: Char,
    val adapter1: Byte,
    val adapter2: Int?,
    val types: List<String>,
    val moshi: String,
    val isStuff: Int,
    val is_stuff: Int,
    val getStuff: Int,
    val get_stuff: Int
)
