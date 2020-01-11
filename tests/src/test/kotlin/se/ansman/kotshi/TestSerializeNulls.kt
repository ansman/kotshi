package se.ansman.kotshi

import com.squareup.moshi.Moshi
import kotlin.test.Test
import kotlin.test.assertEquals

class TestSerializeNulls {
    private val adapter = Moshi.Builder()
        .add(TestFactory)
        .build()
        .adapter(ClassWithSerializeNulls::class.java)

    @Test
    fun root() {
        assertEquals(
            """{"nested":{"value":"v"}}""",
            adapter.toJson(ClassWithSerializeNulls(ClassWithSerializeNulls.Nested("v")))
        )
        assertEquals("""{"nested":null}""", adapter.toJson(ClassWithSerializeNulls(null)))
    }

    @Test
    fun nested() {
        assertEquals(
            """{"nested":{"value":null}}""",
            adapter.toJson(ClassWithSerializeNulls(ClassWithSerializeNulls.Nested(null)))
        )
    }
}