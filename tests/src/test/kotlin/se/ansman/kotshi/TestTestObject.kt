package se.ansman.kotshi

import com.squareup.moshi.Moshi
import kotlin.test.Test
import kotlin.test.assertEquals

class TestTestObject {
    private val adapter = Moshi.Builder()
        .add(TestFactory)
        .build()
        .adapter(TestObject::class.java)

    @Test
    fun reading() {
        assertEquals(TestObject, adapter.fromJson("{}"))
        assertEquals(TestObject, adapter.fromJson("""{"foo":"bar"}"""))
        assertEquals(null, adapter.fromJson("null"))
    }

    @Test
    fun writing() {
        assertEquals("{}", adapter.toJson(TestObject))
        assertEquals("null", adapter.toJson(null))
    }
}