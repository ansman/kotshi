package se.ansman.kotshi

import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SealedClassWithoutDefaultTest {
    private val adapter = Moshi.Builder()
        .add(TestFactory)
        .build()
        .adapter(SealedClassWithoutDefault::class.java)

    @Test
    fun reading_normal() {
        val json = """{"type":"type2","bar":"bar2"}"""
        assertEquals(SealedClassWithoutDefault.Subclass2("bar2"), adapter.fromJson(json))
    }

    @Test
    fun reading_default() {
        assertFailsWith<JsonDataException> { adapter.fromJson("""{"type":"unknown"}""") }
        assertFailsWith<JsonDataException> { adapter.fromJson("{}") }
    }

    @Test
    fun writing_normal() {
        assertEquals("""{"bar":"bar2"}""", adapter.toJson(SealedClassWithoutDefault.Subclass2("bar2")))
    }
}