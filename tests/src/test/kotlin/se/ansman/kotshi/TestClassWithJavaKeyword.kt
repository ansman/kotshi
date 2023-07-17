package se.ansman.kotshi

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.squareup.moshi.JsonWriter
import okio.Buffer
import org.junit.jupiter.api.Test

@InternalKotshiApi
class TestClassWithJavaKeyword {
    private val adapter = KotshiClassWithJavaKeywordJsonAdapter()

    @Test
    fun reading() {
        val json = """{
            |  "default": true,
            |  "int": 4711,
            |  "case": 1337
            |}""".trimMargin()

        assertThat(adapter.fromJson(json))
            .isEqualTo(ClassWithJavaKeyword(true, 4711, 1337))
    }

    @Test
    fun writing() {
        val expected = """{
            |  "default": true,
            |  "int": 4711,
            |  "case": 1337
            |}""".trimMargin()

        val actual = Buffer()
            .apply {
                adapter.toJson(
                    JsonWriter.of(this)
                        .apply {
                            indent = "  "
                        }, ClassWithJavaKeyword(true, 4711, 1337)
                )
            }
            .readUtf8()
        assertThat(actual)
            .isEqualTo(expected)
    }

}