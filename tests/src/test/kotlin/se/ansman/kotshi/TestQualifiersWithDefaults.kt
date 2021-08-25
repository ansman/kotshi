package se.ansman.kotshi

import com.squareup.moshi.Moshi
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestQualifiersWithDefaults {
    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun test() {
        var callCount = 0
        Moshi.Builder()
            .add(KotshiTestFactory)
            .add { type, annotations, moshi ->
                if (type == String::class.java && annotations.isNotEmpty()) {
                    ++callCount
                    val annotation = annotations.single() as QualifierWithArguments
                    assertTrue(arrayOf("vararg").contentEquals(annotation.vararg))
                    assertEquals(true, annotation.booleanArg)
                    assertEquals(124, annotation.byteArg)
                    assertEquals(254u, annotation.ubyteArg)
                    assertEquals(10_000, annotation.shortArg)
                    assertEquals(32_768u, annotation.ushortArg)
                    assertEquals('K', annotation.charArg)
                    assertEquals(100_000, annotation.intArg)
                    assertEquals(2147483648u, annotation.uintArg)
                    assertEquals(100_000_000_000_000, annotation.longArg)
                    assertEquals(9_223_372_036_854_775_808u, annotation.ulongArg)
                    assertEquals(1f, annotation.floatArg)
                    assertEquals(2.0, annotation.doubleArg)
                    assertEquals("string", annotation.stringArg)
                    assertTrue(booleanArrayOf().contentEquals(annotation.emptyArray))
                    assertTrue(booleanArrayOf(true).contentEquals(annotation.booleanArrayArg))
                    assertTrue(byteArrayOf(124).contentEquals(annotation.byteArrayArg))
                    assertTrue(ubyteArrayOf(254u).contentEquals(annotation.ubyteArrayArg))
                    assertTrue(shortArrayOf(10_000).contentEquals(annotation.shortArrayArg))
                    assertTrue(ushortArrayOf(32_768u).contentEquals(annotation.ushortArrayArg))
                    assertTrue(charArrayOf('K').contentEquals(annotation.charArrayArg))
                    assertTrue(intArrayOf(100_000).contentEquals(annotation.intArrayArg))
                    assertTrue(uintArrayOf(2147483648u).contentEquals(annotation.uintArrayArg))
                    assertTrue(longArrayOf(100_000_000_000_000).contentEquals(annotation.longArrayArg))
                    assertTrue(ulongArrayOf(9_223_372_036_854_775_808u).contentEquals(annotation.ulongArrayArg))
                    assertTrue(floatArrayOf(47.11f).contentEquals(annotation.floatArrayArg))
                    assertTrue(doubleArrayOf(13.37).contentEquals(annotation.doubleArrayArg))
                    assertTrue(arrayOf("string").contentEquals(annotation.stringArrayArg))
                    assertEquals(QualifierWithArguments::class, annotation.classArg)
                    assertEquals("nested", annotation.nestedArg.arg)
                    assertEquals(SomeEnum.VALUE3, annotation.enumArg)
                    moshi.adapter(String::class.java)
                } else {
                    null
                }
            }
            .build()
            .adapter(ClassWithQualifierWithArguments::class.java)
        assertEquals(1, callCount)
    }
}