package se.ansman.kotshi

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.squareup.moshi.Moshi
import org.junit.jupiter.api.Test

class TestTestObject {
    private val adapter = Moshi.Builder()
        .add(TestFactory)
        .build()
        .adapter(TestObject::class.java)

    @Test
    fun reading() {
        assertThat(adapter.fromJson("{}")).isEqualTo(TestObject)
        assertThat(adapter.fromJson("""{"foo":"bar"}""")).isEqualTo(TestObject)
        assertThat(adapter.fromJson("null")).isNull()
    }

    @Test
    fun writing() {
        assertThat(adapter.toJson(TestObject)).isEqualTo<String?>("{}")
        assertThat(adapter.toJson(null)).isEqualTo<String?>("null")
    }
}