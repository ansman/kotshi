package se.ansman.kotshi

import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class SealedClassFallbacks {
    private val moshi = Moshi.Builder().add(TestFactory).build()
    private val a1 = moshi.adapter(SealedClassOnInvalidFail::class.java)
    private val a2 = moshi.adapter(SealedClassOnInvalidNull::class.java)
    private val a3 = moshi.adapter(SealedClassOnMissingFail::class.java)
    private val a4 = moshi.adapter(SealedClassOnMissingNull::class.java)

    @Test
    fun invalid_fail() {
        assertFailsWith<JsonDataException> { a1.fromJson("""{"type":"type2"}""") }
        assertEquals(SealedClassOnInvalidFail.Default, a1.fromJson("{}"))
    }

    @Test
    fun invalid_null() {
        assertNull(a2.fromJson("""{"type":"type2"}"""))
        assertEquals(SealedClassOnInvalidNull.Default, a2.fromJson("{}"))
    }

    @Test
    fun missing_fail() {
        assertFailsWith<JsonDataException> { a3.fromJson("{}") }
        assertEquals(SealedClassOnMissingFail.Default, a3.fromJson("""{"type":"type2"}"""))
    }

    @Test
    fun missing_null() {
        assertNull(a4.fromJson("{}"))
        assertEquals(SealedClassOnMissingNull.Default, a4.fromJson("""{"type":"type2"}"""))
    }
}