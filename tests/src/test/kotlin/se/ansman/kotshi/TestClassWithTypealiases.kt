package se.ansman.kotshi

import com.squareup.moshi.Moshi
import org.junit.Test
import kotlin.test.assertEquals

class TestClassWithTypealiases {
    private val adapter = Moshi.Builder()
        .add(TestFactory)
        .build()
        .adapter(ClassWithTypealiases::class.java)

    private val json = """
        {
          "string": "some string",
          "list": [
            "item 1",
            "item 2"
          ],
          "map": {
            "key 1": [
              "value 1"
            ],
            "key 2": [
              "value 2"
            ]
          }
        }
    """.trimIndent()

    private val value = ClassWithTypealiases(
        string = "some string",
        list = listOf("item 1", "item 2"),
        map = mapOf(
            "key 1" to listOf("value 1"),
            "key 2" to listOf("value 2"),
        ),
    )

    @Test
    fun read() {
        assertEquals(value, adapter.fromJson(json))
    }

    @Test
    fun write() {
        assertEquals(adapter.indent("  ").toJson(value), json)
    }
}