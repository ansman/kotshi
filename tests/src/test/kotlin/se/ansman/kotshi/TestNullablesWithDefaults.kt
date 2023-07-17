package se.ansman.kotshi

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import okio.Buffer
import org.junit.jupiter.api.Test

class TestNullablesWithDefaults {
    private val moshi = Moshi.Builder()
        .add(TestFactory)
        .build()

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

        val actual = moshi.adapter(NullablesWithDefaults::class.java).fromJson(
            """
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
        """.trimMargin()
        )

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun withAbsentValues() {
        val expected = NullablesWithDefaults()
        val actual = moshi.adapter(NullablesWithDefaults::class.java).fromJson("{}")
        assertThat(actual).isEqualTo(expected)
    }

    private inline fun <reified T> T.testFormatting(json: String) {
        val adapter = moshi.adapter(T::class.java)
        val actual = adapter.fromJson(json)
        assertThat(actual).isEqualTo(this)
        assertThat(Buffer()
                .apply {
                    JsonWriter.of(this).run {
                        indent = "  "
                        adapter.toJson(this, actual)
                    }
                }
                .readUtf8()).isEqualTo<String>(json)
    }
}