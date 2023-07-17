package se.ansman.kotshi

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import okio.Buffer
import org.junit.jupiter.api.Test

class TestTransientAdapters {
    private val moshi = Moshi.Builder()
        .add(TestFactory)
        .build()

    @Test
    fun withValues() {
        val json = """{
             |  "value2": "string2"
             |}""".trimMargin()

        val expected = ClassWithTransient(
            value = "",
            value2 = "string2",
            list = listOf()
        )

        expected.testFormatting(json)
    }

    @Test
    fun ignoreSupplied() {
        val json = """{
             |  "value": "string",
             |  "value2": "string2",
             |  "list": [ "string3" ]
             |}""".trimMargin()

        val expected = ClassWithTransient(
            value = "",
            value2 = "string2",
            list = listOf()
        )

        assertThat(moshi.adapter(ClassWithTransient::class.java).fromJson(json)).isEqualTo(expected)
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
