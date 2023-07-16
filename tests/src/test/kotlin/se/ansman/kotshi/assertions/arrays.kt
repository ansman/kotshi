@file:OptIn(ExperimentalUnsignedTypes::class)

package se.ansman.kotshi.assertions

import assertk.Assert
import assertk.assertions.isTrue
import assertk.assertions.prop
import assertk.assertions.support.expected

@JvmName("isBooleanArrayEmpty")
fun Assert<BooleanArray>.isEmpty() {
    prop(BooleanArray::isEmpty).isTrue()
}

fun Assert<BooleanArray>.hasContentsEqualTo(expected: BooleanArray) = given {  actual ->
    if (!actual.contentEquals(expected)) {
        expected("Expected to be ${expected.contentToString()} but was ${actual.contentToString()}")
    }
}

@JvmName("isByteArrayEmpty")
fun Assert<ByteArray>.isEmpty() {
    prop(ByteArray::isEmpty).isTrue()
}

fun Assert<ByteArray>.hasContentsEqualTo(expected: ByteArray) = given {  actual ->
    if (!actual.contentEquals(expected)) {
        expected("Expected to be ${expected.contentToString()} but was ${actual.contentToString()}")
    }
}

@JvmName("isUByteArrayEmpty")
fun Assert<UByteArray>.isEmpty() {
    prop(UByteArray::isEmpty).isTrue()
}

fun Assert<UByteArray>.hasContentsEqualTo(expected: UByteArray) = given {  actual ->
    if (!actual.contentEquals(expected)) {
        expected("Expected to be ${expected.contentToString()} but was ${actual.contentToString()}")
    }
}

@JvmName("isCharArrayEmpty")
fun Assert<CharArray>.isEmpty() {
    prop(CharArray::isEmpty).isTrue()
}

fun Assert<CharArray>.hasContentsEqualTo(expected: CharArray) = given {  actual ->
    if (!actual.contentEquals(expected)) {
        expected("Expected to be ${expected.contentToString()} but was ${actual.contentToString()}")
    }
}

@JvmName("isShortArrayEmpty")
fun Assert<ShortArray>.isEmpty() {
    prop(ShortArray::isEmpty).isTrue()
}

fun Assert<ShortArray>.hasContentsEqualTo(expected: ShortArray) = given {  actual ->
    if (!actual.contentEquals(expected)) {
        expected("Expected to be ${expected.contentToString()} but was ${actual.contentToString()}")
    }
}

@JvmName("isUShortArrayEmpty")
fun Assert<UShortArray>.isEmpty() {
    prop(UShortArray::isEmpty).isTrue()
}

fun Assert<UShortArray>.hasContentsEqualTo(expected: UShortArray) = given {  actual ->
    if (!actual.contentEquals(expected)) {
        expected("Expected to be ${expected.contentToString()} but was ${actual.contentToString()}")
    }
}

@JvmName("isIntArrayEmpty")
fun Assert<IntArray>.isEmpty() {
    prop(IntArray::isEmpty).isTrue()
}

fun Assert<IntArray>.hasContentsEqualTo(expected: IntArray) = given { actual ->
    if (!actual.contentEquals(expected)) {
        expected("Expected to be ${expected.contentToString()} but was ${actual.contentToString()}")
    }
}

@JvmName("isUIntArrayEmpty")
fun Assert<UIntArray>.isEmpty() {
    prop(UIntArray::isEmpty).isTrue()
}

fun Assert<UIntArray>.hasContentsEqualTo(expected: UIntArray) = given {  actual ->
    if (!actual.contentEquals(expected)) {
        expected("Expected to be ${expected.contentToString()} but was ${actual.contentToString()}")
    }
}

@JvmName("isLongArrayEmpty")
fun Assert<LongArray>.isEmpty() {
    prop(LongArray::isEmpty).isTrue()
}

fun Assert<LongArray>.hasContentsEqualTo(expected: LongArray) = given {  actual ->
    if (!actual.contentEquals(expected)) {
        expected("Expected to be ${expected.contentToString()} but was ${actual.contentToString()}")
    }
}

@JvmName("isULongArrayEmpty")
fun Assert<ULongArray>.isEmpty() {
    prop(ULongArray::isEmpty).isTrue()
}

fun Assert<ULongArray>.hasContentsEqualTo(expected: ULongArray) = given {  actual ->
    if (!actual.contentEquals(expected)) {
        expected("Expected to be ${expected.contentToString()} but was ${actual.contentToString()}")
    }
}

@JvmName("isFloatArrayEmpty")
fun Assert<FloatArray>.isEmpty() {
    prop(FloatArray::isEmpty).isTrue()
}

fun Assert<FloatArray>.hasContentsEqualTo(expected: FloatArray) = given {  actual ->
    if (!actual.contentEquals(expected)) {
        expected("Expected to be ${expected.contentToString()} but was ${actual.contentToString()}")
    }
}

@JvmName("isDoubleArrayEmpty")
fun Assert<DoubleArray>.isEmpty() {
    prop(DoubleArray::isEmpty).isTrue()
}

fun Assert<DoubleArray>.hasContentsEqualTo(expected: DoubleArray) = given {  actual ->
    if (!actual.contentEquals(expected)) {
        expected("Expected to be ${expected.contentToString()} but was ${actual.contentToString()}")
    }
}

@JvmName("isArrayEmpty")
fun Assert<Array<*>>.isEmpty() {
    prop(Array<*>::isEmpty).isTrue()
}

fun Assert<Array<*>>.hasContentsEqualTo(expected: Array<*>) = given {  actual ->
    if (!actual.contentEquals(expected)) {
        expected("Expected to be ${expected.contentToString()} but was ${actual.contentToString()}")
    }
}