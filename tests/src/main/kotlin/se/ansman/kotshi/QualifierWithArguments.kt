package se.ansman.kotshi

import com.squareup.moshi.JsonQualifier
import kotlin.reflect.KClass


@JsonQualifier
annotation class QualifierWithArguments(
    vararg val vararg: String,
    val booleanArg: Boolean,
    val byteArg: Byte,
    val charArg: Char,
    val shortArg: Short,
    val intArg: Int,
    val longArg: Long,
    val floatArg: Float,
    val doubleArg: Double,
    val stringArg: String,
    val emptyArray: Array<String>,
    val booleanArrayArg: BooleanArray,
    val byteArrayArg: ByteArray,
    val charArrayArg: CharArray,
    val shortArrayArg: ShortArray,
    val intArrayArg: IntArray,
    val longArrayArg: LongArray,
    val floatArrayArg: FloatArray,
    val doubleArrayArg: DoubleArray,
    val stringArrayArg: Array<String>,
    val classArg: KClass<*>,
    val nestedArg: Nested,
    val enumArg: SomeEnum
) {
    annotation class Nested(val arg: String)
}

@JsonSerializable
data class ClassWithQualifierWithArguments(
    @QualifierWithArguments(
        "vararg",
        booleanArg = true,
        byteArg = 254.toByte(),
        shortArg = 10_000,
        charArg = 'K',
        intArg = 100_000,
        longArg = 100_000_000_000_000,
        floatArg = 1f,
        doubleArg = 2.0,
        stringArg = "string",
        emptyArray = [],
        booleanArrayArg = [true],
        byteArrayArg = [254.toByte()],
        shortArrayArg = [10_000],
        charArrayArg = ['K'],
        intArrayArg = [100_000],
        longArrayArg = [100_000_000_000_000],
        floatArrayArg = [47.11f],
        doubleArrayArg = [13.37],
        stringArrayArg = ["string"],
        classArg = QualifierWithArguments::class,
        nestedArg = QualifierWithArguments.Nested("nested"),
        enumArg = SomeEnum.VALUE3
    )
    val foo: String
)

@JsonSerializable
data class ClassWithQualifierWithEscapedArguments(
    @QualifierWithArguments(
        "\"\"",
        booleanArg = true,
        byteArg = 254.toByte(),
        shortArg = 10_000,
        charArg = '\'',
        intArg = 100_000,
        longArg = 100_000_000_000_000,
        floatArg = 1f,
        doubleArg = 2.0,
        stringArg = "\"\"",
        emptyArray = [],
        booleanArrayArg = [true],
        byteArrayArg = [254.toByte()],
        shortArrayArg = [10_000],
        charArrayArg = ['\''],
        intArrayArg = [100_000],
        longArrayArg = [100_000_000_000_000],
        floatArrayArg = [47.11f],
        doubleArrayArg = [13.37],
        stringArrayArg = ["\"\""],
        classArg = QualifierWithArguments::class,
        nestedArg = QualifierWithArguments.Nested("\"\""),
        enumArg = SomeEnum.VALUE3
    )
    val foo: String
)