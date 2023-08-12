package se.ansman.kotshi

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.squareup.moshi.JsonWriter
import okio.Buffer
import org.junit.jupiter.api.Test

@InternalKotshiApi
class TestClassWithEscapedKotlinKeyword {
    private val adapter = KotshiClassWithEscapedKotlinKeywordJsonAdapter()

    @Test
    fun reading() {
        val json = """{
            |  "in": "test"
            |}""".trimMargin()

        assertThat(adapter.fromJson(json))
            .isEqualTo(ClassWithEscapedKotlinKeyword("test"))
    }

    @Test
    fun writing() {
        val expected = """{
            |  "in": "test"
            |}""".trimMargin()

        val actual = Buffer()
            .apply {
                adapter.toJson(
                    JsonWriter.of(this)
                        .apply {
                            indent = "  "
                        }, ClassWithEscapedKotlinKeyword("test")
                )
            }
            .readUtf8()
        assertThat(actual)
            .isEqualTo(expected)
    }

}