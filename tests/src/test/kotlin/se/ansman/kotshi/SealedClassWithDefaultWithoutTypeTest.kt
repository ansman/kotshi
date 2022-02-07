package se.ansman.kotshi

import com.squareup.moshi.Moshi
import org.junit.Test
import kotlin.test.assertEquals

class SealedClassWithDefaultWithoutTypeWithoutTypeTest {
    private val adapter = Moshi.Builder()
        .add(TestFactory)
        .build()
        .adapter(SealedClassWithDefaultWithoutType::class.java)

    @Test
    fun reading_normal() {
        val json = """{"type":"type2","bar":"bar2"}"""
        assertEquals(SealedClassWithDefaultWithoutTypeSubclass2("bar2"), adapter.fromJson(json))
    }

    @Test
    fun reading_withFailOnUnknown() {
        val json = """{"type":"type2","bar":"bar2"}"""
        assertEquals(SealedClassWithDefaultWithoutTypeSubclass2("bar2"), adapter.failOnUnknown().fromJson(json))
    }

    @Test
    fun reading_default() {
        assertEquals(SealedClassWithDefaultWithoutTypeDefault, adapter.fromJson("""{"type":"unknown"}"""))
    }

    @Test
    fun writing_normal() {
        assertEquals("""{"type":"type2","bar":"bar2"}""", adapter.toJson(SealedClassWithDefaultWithoutTypeSubclass2("bar2")))
    }

    @Test
    fun writing_default() {
        assertEquals("{}", adapter.toJson(SealedClassWithDefaultWithoutTypeDefault))
    }
}