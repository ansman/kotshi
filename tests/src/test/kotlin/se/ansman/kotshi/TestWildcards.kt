package se.ansman.kotshi

import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import okio.Buffer
import org.junit.Test
import kotlin.test.assertEquals


class TestWildcards {
    private val moshi: Moshi = Moshi.Builder()
        .add(TestFactory.INSTANCE)
        .add(String::class.java, Hello::class.java, HelloJsonAdapter())
        .build()

    @Test
    fun testAnyBound() {
        val json = """{
        |  "keys": [
        |    {
        |      "a": 1.0,
        |      "b": "value"
        |    },
        |    {
        |      "d": "value",
        |      "e": true
        |    }
        |  ]
        |}""".trimMargin()
        val adapter = moshi.adapter(Wildcards.AnyBound::class.java)
        val actual = adapter.fromJson(json)

        val expected = Wildcards.AnyBound(listOf(
            mapOf("a" to 1.0, "b" to "value"),
            mapOf("d" to "value", "e" to true)
        ))

        assertEquals(expected, actual)
        assertEquals(json, Buffer()
            .apply {
                JsonWriter.of(this).run {
                    indent = "  "
                    adapter.toJson(this, actual)
                }
            }
            .readUtf8())
    }
}
