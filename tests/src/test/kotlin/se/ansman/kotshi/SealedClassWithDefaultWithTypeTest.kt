package se.ansman.kotshi

import com.squareup.moshi.Moshi
import org.junit.Test
import kotlin.test.assertEquals

class SealedClassWithDefaultWithTypeWithoutTypeTest {
    private val adapter = Moshi.Builder()
        .add(TestFactory)
        .build()
        .adapter(SealedClassWithDefaultWithType::class.java)

    @Test
    fun reading_normal() {
        val json = """{"type":"type2","bar":"bar2"}"""
        assertEquals(SealedClassWithDefaultWithType.Subclass2("bar2"), adapter.fromJson(json))
    }

    @Test
    fun reading_default() {
        assertEquals(SealedClassWithDefaultWithType.Default, adapter.fromJson("""{"type":"type4"}"""))
        assertEquals(SealedClassWithDefaultWithType.Default, adapter.fromJson("""{"type":"unknown"}"""))
    }

    @Test
    fun writing_normal() {
        assertEquals("""{"bar":"bar2"}""", adapter.toJson(SealedClassWithDefaultWithType.Subclass2("bar2")))
    }

    @Test
    fun writing_default() {
        assertEquals("{}", adapter.toJson(SealedClassWithDefaultWithType.Default))
    }
}