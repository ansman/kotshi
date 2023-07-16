package se.ansman.kotshi

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.squareup.moshi.Moshi
import org.junit.jupiter.api.Test

class SealedClassWithDefaultWithTypeWithoutTypeTest {
    private val adapter = Moshi.Builder()
        .add(TestFactory)
        .build()
        .adapter(SealedClassWithDefaultWithType::class.java)

    @Test
    fun reading_normal() {
        val json = """{"type":"type2","bar":"bar2"}"""
        assertThat(adapter.fromJson(json))
            .isEqualTo(SealedClassWithDefaultWithType.Subclass2("bar2"))
    }

    @Test
    fun reading_default() {
        assertThat(adapter.fromJson("""{"type":"type4"}"""))
            .isEqualTo(SealedClassWithDefaultWithType.Default)
        assertThat(adapter.fromJson("""{"type":"unknown"}"""))
            .isEqualTo(SealedClassWithDefaultWithType.Default)
    }

    @Test
    fun writing_normal() {
        assertThat(adapter.toJson(SealedClassWithDefaultWithType.Subclass2("bar2")))
            .isEqualTo("""{"type":"type2","bar":"bar2"}""")
    }

    @Test
    fun writing_default() {
        assertThat("""{"type":"type4"}""")
            .isEqualTo(adapter.toJson(SealedClassWithDefaultWithType.Default))
    }
}