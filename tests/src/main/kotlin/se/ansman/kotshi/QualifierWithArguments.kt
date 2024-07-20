package se.ansman.kotshi

import com.squareup.moshi.JsonQualifier
import kotlin.reflect.KClass


@JsonQualifier
@OptIn(ExperimentalUnsignedTypes::class)
annotation class QualifierWithArguments(
    vararg val vararg: String,
    val booleanArg: Boolean,
    val byteArg: Byte,
//    val ubyteArg: UByte,
    val charArg: Char,
    val shortArg: Short,
//    val ushortArg: UShort,
    val intArg: Int,
//    val uintArg: UInt,
    val longArg: Long,
//    val ulongArg: ULong,
    val floatArg: Float,
    val doubleArg: Double,
    val stringArg: String,
    val emptyArray: BooleanArray,
    val booleanArrayArg: BooleanArray,
    val byteArrayArg: ByteArray,
//    val ubyteArrayArg: UByteArray,
    val charArrayArg: CharArray,
    val shortArrayArg: ShortArray,
//    val ushortArrayArg: UShortArray,
    val intArrayArg: IntArray,
//    val uintArrayArg: UIntArray,
    val longArrayArg: LongArray,
//    val ulongArrayArg: ULongArray,
    val floatArrayArg: FloatArray,
    val doubleArrayArg: DoubleArray,
    val stringArrayArg: Array<String>,
    val classArg: KClass<*>,
    val nestedArg: Nested,
    val enumArg: SomeEnum
) {
    annotation class Nested(val arg: String)
}

@OptIn(ExperimentalUnsignedTypes::class)
@JsonSerializable
data class ClassWithQualifierWithArguments(
    @QualifierWithArguments(
        "vararg",
        booleanArg = true,
        byteArg = 124,
//        ubyteArg = 254u,
        shortArg = 10_000,
//        ushortArg = 32_768u,
        charArg = 'K',
        intArg = 100_000,
//        uintArg = 2147483648u,
        longArg = 100_000_000_000_000,
//        ulongArg = 9_223_372_036_854_775_808u,
        floatArg = 1f,
        doubleArg = 2.0,
        stringArg = "string",
        emptyArray = [],
        booleanArrayArg = [true],
        byteArrayArg = [124],
//        ubyteArrayArg = [254u],
        shortArrayArg = [10_000],
//        ushortArrayArg = [32_768u],
        charArrayArg = ['K'],
        intArrayArg = [100_000],
//        uintArrayArg = [2147483648u],
        longArrayArg = [100_000_000_000_000],
//        ulongArrayArg = [9_223_372_036_854_775_808u],
        floatArrayArg = [47.11f],
        doubleArrayArg = [13.37],
        stringArrayArg = ["string"],
        classArg = QualifierWithArguments::class,
        nestedArg = QualifierWithArguments.Nested("nested"),
        enumArg = SomeEnum.VALUE3
    )
    val foo: String
)

@OptIn(ExperimentalUnsignedTypes::class)
@JsonSerializable
data class ClassWithQualifierWithEscapedArguments(
    @QualifierWithArguments(
        "\"\"",
        booleanArg = true,
        byteArg = 124,
//        ubyteArg = 254u,
        shortArg = 10_000,
//        ushortArg = 32_768u,
        charArg = '\'',
        intArg = 100_000,
//        uintArg = 2147483648u,
        longArg = 100_000_000_000_000,
//        ulongArg = 9_223_372_036_854_775_808u,
        floatArg = 1f,
        doubleArg = 2.0,
        stringArg = "\"\"",
        emptyArray = [],
        booleanArrayArg = [true],
        byteArrayArg = [124],
//        ubyteArrayArg = [254u],
        shortArrayArg = [10_000],
//        ushortArrayArg = [32_768u],
        charArrayArg = ['\''],
        intArrayArg = [100_000],
//        uintArrayArg = [2147483648u],
        longArrayArg = [100_000_000_000_000],
//        ulongArrayArg = [9_223_372_036_854_775_808u],
        floatArrayArg = [47.11f],
        doubleArrayArg = [13.37],
        stringArrayArg = ["\"\""],
        classArg = QualifierWithArguments::class,
        nestedArg = QualifierWithArguments.Nested("\"\""),
        enumArg = SomeEnum.VALUE3
    )
    val foo: String
)