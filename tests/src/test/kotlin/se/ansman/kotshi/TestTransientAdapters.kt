package se.ansman.kotshi

import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import okio.Buffer
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class TestTransientAdapters {
    private lateinit var moshi: Moshi

    @Before
    fun setup() {
        moshi = Moshi.Builder()
            .add(TestFactory.INSTANCE)
            .build()
    }

    @Test
    fun withValues() {
        val json = """{
             |  "value2": "string2"
             |}""".trimMargin()

        val expected = ClassWithTransient(
            value = "",
            value2 = "string2",
            list = listOf())

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
                list = listOf())

        assertEquals(expected, moshi.adapter(ClassWithTransient::class.java).fromJson(json))
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
