package se.ansman.kotshi

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import okio.Buffer
import org.junit.jupiter.api.Test

class TestPrimitiveDefaultValues {
    private val moshi = Moshi.Builder()
        .add(TestFactory)
        .build()

    @Test
    fun withValues() {
        val json = """{
             |  "someString": "someString",
             |  "someBoolean": false,
             |  "someByte": 255,
             |  "someChar": "X",
             |  "someShort": 1337,
             |  "someInt": 1337,
             |  "someLong": 1337,
             |  "someFloat": 0.0,
             |  "someDouble": 0.0
             |}""".trimMargin()

        val expected = ClassWithPrimitiveDefaults(
            someString = "someString",
            someBoolean = false,
            someByte = -1,
            someChar = 'X',
            someShort = 1337,
            someInt = 1337,
            someLong = 1337,
            someFloat = 0f,
            someDouble = 0.0
        )

        expected.testFormatting(json)
    }

    @Test
    fun withNullValues() {
        val expected = ClassWithPrimitiveDefaults(
            someString = "default",
            someBoolean = true,
            someByte = 66,
            someChar = 'N',
            someShort = 4711,
            someInt = 4711,
            someLong = 4711,
            someFloat = 0.4711f,
            someDouble = 0.4711
        )

        val actual = moshi.adapter(ClassWithPrimitiveDefaults::class.java).fromJson(
            """{
             |  "someString": null,
             |  "someBoolean": null,
             |  "someByte": null,
             |  "someChar": null,
             |  "someShort": null,
             |  "someInt": null,
             |  "someLong": null,
             |  "someFloat": null,
             |  "someDouble": null
             |}""".trimMargin()
        )

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun withAbsentValues() {
        val expected = ClassWithPrimitiveDefaults(
            someString = "default",
            someBoolean = true,
            someByte = 66,
            someChar = 'N',
            someShort = 4711,
            someInt = 4711,
            someLong = 4711,
            someFloat = 0.4711f,
            someDouble = 0.4711
        )

        val actual = moshi.adapter(ClassWithPrimitiveDefaults::class.java).fromJson("{}")
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