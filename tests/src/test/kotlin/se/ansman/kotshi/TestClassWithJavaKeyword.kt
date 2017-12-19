package se.ansman.kotshi

import com.squareup.moshi.JsonWriter
import okio.Buffer
import org.junit.Test
import kotlin.test.assertEquals

class TestClassWithJavaKeyword {
    private val adapter = KotshiClassWithJavaKeywordJsonAdapter()

    @Test
    fun reading() {
        val json = """{
            |  "default": true,
            |  "int": 4711,
            |  "case": 1337
            |}""".trimMargin()

        assertEquals(ClassWithJavaKeyword(true, 4711, 1337), adapter.fromJson(json))
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
                    adapter.toJson(JsonWriter.of(this)
                            .apply {
                                indent = "  "
                            }, ClassWithJavaKeyword(true, 4711, 1337))
                }
                .readUtf8()
        assertEquals(expected, actual)
    }

}