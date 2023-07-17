package se.ansman.kotshi

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import okio.Buffer
import org.junit.jupiter.api.Test


class TestWildcards {
    private val moshi: Moshi = Moshi.Builder()
        .add(TestFactory)
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

        val expected = Wildcards.AnyBound(
            listOf(
                mapOf("a" to 1.0, "b" to "value"),
                mapOf("d" to "value", "e" to true)
            )
        )

        assertThat(actual).isEqualTo(expected)
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
