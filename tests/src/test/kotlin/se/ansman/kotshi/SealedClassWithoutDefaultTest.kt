package se.ansman.kotshi

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import org.junit.jupiter.api.Test

class SealedClassWithoutDefaultTest {
    private val adapter = Moshi.Builder()
        .add(TestFactory)
        .build()
        .adapter(SealedClassWithoutDefault::class.java)

    @Test
    fun reading_normal() {
        val json = """{"type":"type2","bar":"bar2"}"""
        assertThat(adapter.fromJson(json))
            .isEqualTo(SealedClassWithoutDefault.Subclass2("bar2"))
    }

    @Test
    fun reading_default() {
        assertFailure { adapter.fromJson("""{"type":"unknown"}""") }
            .isInstanceOf<JsonDataException>()
        assertFailure { adapter.fromJson("{}") }
            .isInstanceOf<JsonDataException>()
    }

    @Test
    fun writing_normal() {
        assertThat(adapter.toJson(SealedClassWithoutDefault.Subclass2("bar2")))
            .isEqualTo("""{"type":"type2","bar":"bar2"}""")
    }

    @Test
    fun failOnUnknown() {
        val json = """{"baz":"baz3","type":"type3"}"""
        assertThat(adapter.failOnUnknown().fromJson(json))
            .isEqualTo(SealedClassWithoutDefault.Subclass3("baz3", "type3"))
    }
}