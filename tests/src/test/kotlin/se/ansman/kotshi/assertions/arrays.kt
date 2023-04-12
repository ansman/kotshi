@file:OptIn(ExperimentalUnsignedTypes::class)

package se.ansman.kotshi.assertions

import assertk.Assert
import assertk.assertions.isTrue
import assertk.assertions.prop

@JvmName("isBooleanArrayEmpty")
fun Assert<BooleanArray>.isEmpty() {
    prop(BooleanArray::isEmpty).isTrue()
}

@JvmName("isByteArrayEmpty")
fun Assert<ByteArray>.isEmpty() {
    prop(ByteArray::isEmpty).isTrue()
}

@JvmName("isUByteArrayEmpty")
fun Assert<UByteArray>.isEmpty() {
    prop(UByteArray::isEmpty).isTrue()
}

@JvmName("isCharArrayEmpty")
fun Assert<CharArray>.isEmpty() {
    prop(CharArray::isEmpty).isTrue()
}

@JvmName("isShortArrayEmpty")
fun Assert<ShortArray>.isEmpty() {
    prop(ShortArray::isEmpty).isTrue()
}

@JvmName("isUShortArrayEmpty")
fun Assert<UShortArray>.isEmpty() {
    prop(UShortArray::isEmpty).isTrue()
}

@JvmName("isIntArrayEmpty")
fun Assert<IntArray>.isEmpty() {
    prop(IntArray::isEmpty).isTrue()
}

@JvmName("isUIntArrayEmpty")
fun Assert<UIntArray>.isEmpty() {
    prop(UIntArray::isEmpty).isTrue()
}

@JvmName("isLongArrayEmpty")
fun Assert<LongArray>.isEmpty() {
    prop(LongArray::isEmpty).isTrue()
}

@JvmName("isULongArrayEmpty")
fun Assert<ULongArray>.isEmpty() {
    prop(ULongArray::isEmpty).isTrue()
}

@JvmName("isFloatArrayEmpty")
fun Assert<FloatArray>.isEmpty() {
    prop(FloatArray::isEmpty).isTrue()
}

@JvmName("isDoubleArrayEmpty")
fun Assert<DoubleArray>.isEmpty() {
    prop(DoubleArray::isEmpty).isTrue()
}

@JvmName("isArrayEmpty")
fun Assert<Array<*>>.isEmpty() {
    prop(Array<*>::isEmpty).isTrue()
}