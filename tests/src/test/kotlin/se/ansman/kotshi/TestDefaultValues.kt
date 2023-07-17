package se.ansman.kotshi

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import okio.Buffer
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class TestDefaultValues {
    private val moshi = Moshi.Builder()
        .add(TestFactory)
        .add(LocalDate::class.java, LocalDateAdapter)
        .add(LocalTime::class.java, LocalTimeAdapter)
        .add(LocalDateTime::class.java, LocalDateTimeAdapter)
        .build()

    @Test
    fun withValues() {
        val json = """
            |{
            |  "v1": 1,
            |  "v2": "2",
            |  "v3": 3,
            |  "v4": 4,
            |  "v5": 5,
            |  "v6": 6.0,
            |  "v7": 7.0,
            |  "v8": "8",
            |  "v9": [
            |    "9"
            |  ],
            |  "v10": "10"
            |}
        """.trimMargin()

        val expected = ClassWithDefaultValues(
            v1 = 1,
            v2 = '2',
            v3 = 3,
            v4 = 4,
            v5 = 5,
            v6 = 6f,
            v7 = 7.0,
            v8 = "8",
            v9 = listOf("9"),
            v10 = "10"
        )

        expected.testFormatting(json)
    }

    @Test
    fun withNullValues() {
        val expected = ClassWithDefaultValues(v10 = "10")

        val actual = moshi.adapter(ClassWithDefaultValues::class.java).fromJson(
            """{
             |  "v1": null,
             |  "v2": null,
             |  "v3": null,
             |  "v4": null,
             |  "v5": null,
             |  "v6": null,
             |  "v7": null,
             |  "v8": null,
             |  "v9": null,
             |  "v10": "10"
             |}""".trimMargin()
        )

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun withAbsentValues() {
        val expected = ClassWithDefaultValues(v10 = "10")
        val actual = moshi.adapter(ClassWithDefaultValues::class.java).fromJson(
            """
            |{
            |  "v10": "10"
            |}
        """.trimMargin()
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun throwsJsonDataExceptionWhenNotUsingDefaultValues() {
        assertFailure { moshi.adapter(ClassWithDefaultValues::class.java).fromJson("{}") }
            .isInstanceOf<JsonDataException>()
            .hasMessage("The following properties were null: v10 (at path $)")
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

    object LocalDateAdapter : JsonAdapter<LocalDate>() {
        override fun fromJson(reader: JsonReader): LocalDate? =
            if (reader.peek() == JsonReader.Token.NULL) reader.nextNull() else LocalDate.parse(reader.nextString())

        override fun toJson(writer: JsonWriter, value: LocalDate?) {
            writer.value(value?.toString())
        }
    }

    object LocalTimeAdapter : JsonAdapter<LocalTime>() {
        override fun fromJson(reader: JsonReader): LocalTime? =
            if (reader.peek() == JsonReader.Token.NULL) reader.nextNull() else LocalTime.parse(reader.nextString())

        override fun toJson(writer: JsonWriter, value: LocalTime?) {
            writer.value(value?.toString())
        }
    }

    object LocalDateTimeAdapter : JsonAdapter<LocalDateTime>() {
        override fun fromJson(reader: JsonReader): LocalDateTime? =
            if (reader.peek() == JsonReader.Token.NULL) reader.nextNull() else LocalDateTime.parse(reader.nextString())

        override fun toJson(writer: JsonWriter, value: LocalDateTime?) {
            writer.value(value?.toString())
        }
    }
}