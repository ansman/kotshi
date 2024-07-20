package se.ansman.kotshi

import com.squareup.moshi.JsonQualifier
import kotlin.reflect.KClass

@JsonQualifier
annotation class QualifierWithArrays @ExperimentalUnsignedTypes constructor(
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
    val enumArrayArg: Array<Polymorphic.Fallback>,
    val annotationArrayArg: Array<Polymorphic>,
    val classArrayArg: Array<KClass<*>>,
)