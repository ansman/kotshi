package se.ansman.kotshi

@Suppress("ArrayInDataClass")
@JsonSerializable
data class ClassWithArray(
    val byteArray: ByteArray,
    val charArray: CharArray,
    val shortArray: ShortArray,
    val intArray: IntArray,
    val longArray: LongArray,
    val floatArray: FloatArray,
    val doubleArray: DoubleArray,
    val stringArray: Array<String>
)