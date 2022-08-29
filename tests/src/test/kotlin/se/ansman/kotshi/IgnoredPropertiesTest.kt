package se.ansman.kotshi

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import org.junit.Test

class IgnoredPropertiesTest {
    private val moshi = Moshi.Builder().add(TestFactory).build()
    @OptIn(ExperimentalStdlibApi::class)
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