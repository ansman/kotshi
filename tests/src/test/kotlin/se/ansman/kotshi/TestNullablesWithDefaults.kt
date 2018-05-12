package se.ansman.kotshi

import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import okio.Buffer
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class TestNullablesWithDefaults {
    private lateinit var moshi: Moshi

    @Before
    fun setup() {
        moshi = Moshi.Builder()
            .add(TestFactory)
            .build()
    }

    @Test
    fun withValues() {
        val json = """
            |{
            |  "v1": 1,
            |  "v2": "2",
            |  "v3": 3,
            |  "v4": 4,
            |  "v5": 5,
            |  "v6": 6.0,
            |  "v7": 7.0,
            |  "v8": "n/a",
            |  "v9": [
            |    "Hello"
            |  ]
            |}
        """.trimMargin()

        val expected = NullablesWithDefaults(
            v1 = 1,
            v2 = '2',
            v3 = 3,
            v4 = 4,
            v5 = 5L,
            v6 = 6f,
            v7 = 7.0,
            v8 = "n/a",
            v9 = listOf("Hello")
        )

        expected.testFormatting(json)
    }

    @Test
    fun withNullValues() {
        val expected = NullablesWithDefaults(
            v1 = null,
            v2 = null,
            v3 = null,
            v4 = null,
            v5 = null,
            v6 = null,
            v7 = null,
            v8 = null,
            v9 = null
        )

        val actual = moshi.adapter(NullablesWithDefaults::class.java).fromJson("""
            |{
            |  "v1": null,
            |  "v2": null,
            |  "v3": null,
            |  "v4": null,
            |  "v5": null,
            |  "v6": null,
            |  "v7": null,
            |  "v8": null,
            |  "v9": null
            |}
        """.trimMargin())

        assertEquals(expected, actual)
    }

    @Test
    fun withAbsentValues() {
        val expected = NullablesWithDefaults()
        val actual = moshi.adapter(NullablesWithDefaults::class.java).fromJson("{}")
        assertEquals(expected, actual)
    }

    private inline fun <reified T> T.testFormatting(json: String) {
        val adapter = moshi.adapter(T::class.java)
        val actual = adapter.fromJson(json)
        assertEquals(this, actual)
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