package se.ansman.kotshi

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.squareup.moshi.Moshi
import org.junit.jupiter.api.Test

class SealedClassWithDefaultWithoutTypeWithoutTypeTest {
    private val adapter = Moshi.Builder()
        .add(TestFactory)
        .build()
        .adapter(SealedClassWithDefaultWithoutType::class.java)

    @Test
    fun reading_normal() {
        val json = """{"type":"type2","bar":"bar2"}"""
        assertThat(adapter.fromJson(json))
            .isEqualTo(SealedClassWithDefaultWithoutTypeSubclass2("bar2"))
    }

    @Test
    fun reading_withFailOnUnknown() {
        val json = """{"type":"type2","bar":"bar2"}"""
        assertThat(adapter.failOnUnknown().fromJson(json))
            .isEqualTo(SealedClassWithDefaultWithoutTypeSubclass2("bar2"))
    }

    @Test
    fun reading_default() {
        assertThat(adapter.fromJson("""{"type":"unknown"}"""))
            .isEqualTo(SealedClassWithDefaultWithoutTypeDefault)
    }

    @Test
    fun writing_normal() {
        assertThat(adapter.toJson(SealedClassWithDefaultWithoutTypeSubclass2("bar2")))
            .isEqualTo("""{"type":"type2","bar":"bar2"}""")
    }

    @Test
    fun writing_default() {
        assertThat(adapter.toJson(SealedClassWithDefaultWithoutTypeDefault))
            .isEqualTo("{}")
    }
}