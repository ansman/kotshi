package se.ansman.kotshi

import com.squareup.moshi.*
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Test

private const val HELLO_PREFIX = "Hello, "

class HelloJsonAdapter : JsonAdapter<String>() {
    override fun toJson(writer: JsonWriter, value: String?) {
        if (value == null) {
            writer.nullValue()
        } else {
            writer.value(value.removePrefix(HELLO_PREFIX))
        }
    }

    override fun fromJson(reader: JsonReader): String? = when (reader.peek()) {
        JsonReader.Token.NULL -> reader.nextNull()
        else -> HELLO_PREFIX + reader.nextString()
    }
}

class TestAdapterGeneration {
    val moshi: Moshi = Moshi.Builder()
            .add(TestFactory.INSTANCE)
            .add(String::class.java, Hello::class.java, HelloJsonAdapter())
            .build()

    @Test
    fun testBasic() {
        val json = """{
        |  "string": "string",
        |  "nullableString": "nullableString",
        |  "integer": 4711,
        |  "nullableInt": 1337,
        |  "isBoolean": true,
        |  "isNullableBoolean": false,
        |  "list": [
        |    "String1",
        |    "String2"
        |  ],
        |  "nestedList": [
        |    {
        |      "key1": [
        |        "set1",
        |        "set2"
        |      ]
        |    },
        |    {
        |      "key2": [
        |        "set1",
        |        "set2"
        |      ],
        |      "key3": []
        |    }
        |  ],
        |  "abstractProperty": "abstract",
        |  "other_name": "other_value",
        |  "annotated": "World!",
        |  "genericClass": {
        |    "collection": [
        |      "val1",
        |      "val2"
        |    ],
        |    "value": "val3"
        |  }
        |}""".trimMargin()
        val adapter = moshi.adapter(TestClass::class.java)
        val actual = adapter.fromJson(json)

        val expected = TestClass(
                string = "string",
                nullableString = "nullableString",
                integer = 4711,
                nullableInt = 1337,
                isBoolean = true,
                isNullableBoolean = false,
                list = listOf("String1", "String2"),
                nestedList = listOf(
                        mapOf("key1" to setOf("set1", "set2")),
                        mapOf(
                                "key2" to setOf("set1", "set2"),
                                "key3" to setOf<String>())),
                abstractProperty = "abstract",
                customName = "other_value",
                annotated = "Hello, World!",
                genericClass = GenericClass(listOf("val1", "val2"), "val3"))

        assertEquals(expected, actual)
        assertEquals(json, Buffer()
                .apply {
                    JsonWriter.of(this).run {
                        indent = "  "
                        adapter.toJson(this, actual)
                    }
                }.readUtf8())
    }

    @Test
    fun testNull() {
        try {
            moshi.adapter(TestClass::class.java).fromJson("{}")
        } catch (e: NullPointerException) {
            assertEquals("The following properties were null: string, integer, isBoolean, list, nestedList, abstractProperty, customName, annotated, genericClass", e.message)
        }
    }

    @Test
    fun testCustomNames() {
        val json = """{"jsonProp1":"value1","jsonProp2":"value2"}"""
        val adapter = moshi.adapter(CustomNames::class.java)
        val actual = adapter.fromJson(json)
        val expected = CustomNames("value1", "value2")
        assertEquals(expected, actual)
        assertEquals(json, adapter.toJson(actual))
    }

    @Test
    fun testExtraFields() {
        val adapter = moshi.adapter(Simple::class.java)
        val actual = adapter.fromJson("""{"prop":"value","extra_prop":"extra_value"}""")
        assertEquals(Simple("value"), actual)
        assertEquals("""{"prop":"value"}""", adapter.toJson(actual))
    }
}