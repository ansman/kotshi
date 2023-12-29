package se.ansman.kotshi

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.junit.jupiter.api.Test

class IgnoredPropertiesTest {
    private val moshi = Moshi.Builder().add(TestFactory).build()
    private val adapter: JsonAdapter<IgnoredProperties> = moshi.adapter()

    @Test
    fun reading() {
        assertThat(adapter.fromJson("""{"property1":"value"}"""))
            .isEqualTo(IgnoredProperties("value"))
    }

    @Test
    fun writing() {
        assertThat(adapter.toJson(IgnoredProperties("value")))
            .isEqualTo("""{"property1":"value"}""")
    }
}