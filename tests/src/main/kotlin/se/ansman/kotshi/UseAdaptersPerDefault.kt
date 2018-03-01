package se.ansman.kotshi

@JsonSerializable(useAdaptersForPrimitives = PrimitiveAdapters.ENABLED)
data class UsingPrimitiveAdapterTestClass(
    val aString: String,
    val aBoolean: Boolean,
    val aNullableBoolean: Boolean?,
    val aByte: Byte,
    val nullableByte: Byte?,
    val aChar: Char,
    val nullableChar: Char?,
    val aShort: Short,
    val nullableShort: Short?,
    val integer: Int,
    val nullableInteger: Int?,
    val aLong: Long,
    val nullableLong: Long?,
    val aFloat: Float,
    val nullableFloat: Float?,
    val aDouble: Double,
    val nullableDouble: Double?
)

@JsonSerializable(useAdaptersForPrimitives = PrimitiveAdapters.DISABLED)
data class NotUsingPrimitiveAdapterTestClass(
    val aString: String,
    val aBoolean: Boolean,
    val aNullableBoolean: Boolean?,
    val aByte: Byte,
    val nullableByte: Byte?,
    val aChar: Char,
    val nullableChar: Char?,
    val aShort: Short,
    val nullableShort: Short?,
    val integer: Int,
    val nullableInteger: Int?,
    val aLong: Long,
    val nullableLong: Long?,
    val aFloat: Float,
    val nullableFloat: Float?,
    val aDouble: Double,
    val nullableDouble: Double?
)

@JsonSerializable(useAdaptersForPrimitives = PrimitiveAdapters.DISABLED)
data class PrimitiveWithJsonQualifierTestClass(
    @Hello val greetingInt: Int
)
