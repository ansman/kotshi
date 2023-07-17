package se.ansman.kotshi

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SealedClassFallbacks {
    private val moshi = Moshi.Builder().add(TestFactory).build()
    private val a1 = moshi.adapter(SealedClassOnInvalidFail::class.java)
    private val a2 = moshi.adapter(SealedClassOnInvalidNull::class.java)
    private val a3 = moshi.adapter(SealedClassOnMissingFail::class.java)
    private val a4 = moshi.adapter(SealedClassOnMissingNull::class.java)

    @Test
    fun invalid_fail() {
        assertFailure { a1.fromJson("""{"type":"type2"}""") }
            .isInstanceOf<JsonDataException>()
        assertThat(a1.fromJson("{}"))
            .isEqualTo(SealedClassOnInvalidFail.Default)
    }

    @Test
    fun invalid_null() {
        assertNull(a2.fromJson("""{"type":"type2"}"""))
        assertThat(a2.fromJson("{}"))
            .isEqualTo(SealedClassOnInvalidNull.Default)
    }

    @Test
    fun missing_fail() {
        assertFailure { a3.fromJson("{}") }
            .isInstanceOf<JsonDataException>()
        assertThat(a3.fromJson("""{"type":"type2"}"""))
            .isEqualTo(SealedClassOnMissingFail.Default)
    }

    @Test
    fun missing_null() {
        assertNull(a4.fromJson("{}"))
        assertThat(a4.fromJson("""{"type":"type2"}"""))
            .isEqualTo(SealedClassOnMissingNull.Default)
    }
}