package se.ansman.kotshi

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import okio.Buffer
import org.junit.jupiter.api.Test

class TestPrimitiveAdapters {
    private val basicMoshi = Moshi.Builder().build()
    private val stringAdapter = DelegateAdapter(basicMoshi.adapter(String::class.java))
    private val booleanAdapter = DelegateAdapter(basicMoshi.adapter(Boolean::class.java))
    private val byteAdapter = DelegateAdapter(basicMoshi.adapter(Byte::class.java))
    private val charAdapter = DelegateAdapter(basicMoshi.adapter(Char::class.java))
    private val shortAdapter = DelegateAdapter(basicMoshi.adapter(Short::class.java))
    private val intAdapter = DelegateAdapter(basicMoshi.adapter(Int::class.java))
    private val longAdapter = DelegateAdapter(basicMoshi.adapter(Long::class.java))
    private val floatAdapter = DelegateAdapter(basicMoshi.adapter(Float::class.java))
    private val doubleAdapter = DelegateAdapter(basicMoshi.adapter(Double::class.java))
    private val moshi = Moshi.Builder()
        .add(TestFactory)
        .add(String::class.java, stringAdapter)
        .add(Boolean::class.javaPrimitiveType!!, booleanAdapter)
        .add(Boolean::class.javaObjectType, booleanAdapter)
        .add(Byte::class.javaPrimitiveType!!, byteAdapter)
        .add(Byte::class.javaObjectType, byteAdapter)
        .add(Char::class.javaPrimitiveType!!, charAdapter)
        .add(Char::class.javaObjectType, charAdapter)
        .add(Short::class.javaPrimitiveType!!, shortAdapter)
        .add(Short::class.javaObjectType, shortAdapter)
        .add(Int::class.javaPrimitiveType!!, intAdapter)
        .add(Int::class.javaObjectType, intAdapter)
        .add(Long::class.javaPrimitiveType!!, longAdapter)
        .add(Long::class.javaObjectType, longAdapter)
        .add(Float::class.javaPrimitiveType!!, floatAdapter)
        .add(Float::class.javaObjectType, floatAdapter)
        .add(Double::class.javaPrimitiveType!!, doubleAdapter)
        .add(Double::class.javaObjectType, doubleAdapter)
        .add(Int::class.javaObjectType, Hello::class.java, intAdapter)
        .build()

    @Test
    fun testDoesntCallAdapter() {
        testFormatting(
            json, NotUsingPrimitiveAdapterTestClass(
                aString = "hello",
                aBoolean = true,
                aNullableBoolean = false,
                aByte = -1,
                nullableByte = Byte.MIN_VALUE,
                aChar = 'c',
                nullableChar = 'n',
                aShort = 32767,
                nullableShort = -32768,
                integer = 4711,
                nullableInteger = 1337,
                aLong = 4711,
                nullableLong = 1337,
                aFloat = 4711.5f,
                nullableFloat = 1337.5f,
                aDouble = 4711.5,
                nullableDouble = 1337.5
            )
        )
        assertThat(stringAdapter.readCount).isEqualTo(0)
        assertThat(stringAdapter.writeCount).isEqualTo(0)
        assertThat(booleanAdapter.readCount).isEqualTo(0)
        assertThat(booleanAdapter.writeCount).isEqualTo(0)
        assertThat(byteAdapter.readCount).isEqualTo(0)
        assertThat(byteAdapter.writeCount).isEqualTo(0)
        assertThat(charAdapter.readCount).isEqualTo(0)
        assertThat(charAdapter.writeCount).isEqualTo(0)
        assertThat(shortAdapter.readCount).isEqualTo(0)
        assertThat(shortAdapter.writeCount).isEqualTo(0)
        assertThat(intAdapter.readCount).isEqualTo(0)
        assertThat(intAdapter.writeCount).isEqualTo(0)
        assertThat(longAdapter.readCount).isEqualTo(0)
        assertThat(longAdapter.writeCount).isEqualTo(0)
        assertThat(floatAdapter.readCount).isEqualTo(0)
        assertThat(floatAdapter.writeCount).isEqualTo(0)
        assertThat(doubleAdapter.readCount).isEqualTo(0)
        assertThat(doubleAdapter.writeCount).isEqualTo(0)
    }

    @Test
    fun testCallsAdapter() {
        testFormatting(
            json, UsingPrimitiveAdapterTestClass(
                aString = "hello",
                aBoolean = true,
                aNullableBoolean = false,
                aByte = -1,
                nullableByte = Byte.MIN_VALUE,
                aChar = 'c',
                nullableChar = 'n',
                aShort = 32767,
                nullableShort = -32768,
                integer = 4711,
                nullableInteger = 1337,
                aLong = 4711,
                nullableLong = 1337,
                aFloat = 4711.5f,
                nullableFloat = 1337.5f,
                aDouble = 4711.5,
                nullableDouble = 1337.5
            )
        )
        assertThat(stringAdapter.readCount).isEqualTo(1)
        assertThat(stringAdapter.writeCount).isEqualTo(1)
        assertThat(booleanAdapter.readCount).isEqualTo(2)
        assertThat(booleanAdapter.writeCount).isEqualTo(2)
        assertThat(byteAdapter.readCount).isEqualTo(2)
        assertThat(byteAdapter.writeCount).isEqualTo(2)
        assertThat(charAdapter.readCount).isEqualTo(2)
        assertThat(charAdapter.writeCount).isEqualTo(2)
        assertThat(shortAdapter.readCount).isEqualTo(2)
        assertThat(shortAdapter.writeCount).isEqualTo(2)
        assertThat(intAdapter.readCount).isEqualTo(2)
        assertThat(intAdapter.writeCount).isEqualTo(2)
        assertThat(longAdapter.readCount).isEqualTo(2)
        assertThat(longAdapter.writeCount).isEqualTo(2)
        assertThat(floatAdapter.readCount).isEqualTo(2)
        assertThat(floatAdapter.writeCount).isEqualTo(2)
        assertThat(doubleAdapter.readCount).isEqualTo(2)
        assertThat(doubleAdapter.writeCount).isEqualTo(2)
    }

    @Test
    fun callsAdapterWhenQualifiersPresent() {
        testFormatting(
            """{
          |  "greetingInt": 1
          |}""".trimMargin(), PrimitiveWithJsonQualifierTestClass(1)
        )
        assertThat(intAdapter.readCount).isEqualTo(1)
        assertThat(intAdapter.writeCount).isEqualTo(1)
    }

    private inline fun <reified T> testFormatting(json: String, expected: T) {
        val adapter = moshi.adapter(T::class.java)
        val actual = adapter.fromJson(json)
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

    companion object {
        val json = """{
            |  "aString": "hello",
            |  "aBoolean": true,
            |  "aNullableBoolean": false,
            |  "aByte": 255,
            |  "nullableByte": 128,
            |  "aChar": "c",
            |  "nullableChar": "n",
            |  "aShort": 32767,
            |  "nullableShort": -32768,
            |  "integer": 4711,
            |  "nullableInteger": 1337,
            |  "aLong": 4711,
            |  "nullableLong": 1337,
            |  "aFloat": 4711.5,
            |  "nullableFloat": 1337.5,
            |  "aDouble": 4711.5,
            |  "nullableDouble": 1337.5
            |}""".trimMargin()

    }

    private class DelegateAdapter<T>(private val delegate: JsonAdapter<T>) : JsonAdapter<T>() {

        var writeCount: Int = 0
            private set

        var readCount: Int = 0
            private set

        override fun toJson(writer: JsonWriter, value: T?) {
            writeCount += 1
            delegate.toJson(writer, value)
        }

        override fun fromJson(reader: JsonReader): T? {
            readCount += 1
            return delegate.fromJson(reader)
        }
    }
}