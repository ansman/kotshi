package se.ansman.kotshi

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.squareup.moshi.Moshi
import org.junit.jupiter.api.Test

class SealedClassWithExistingLabelTest {
    private val adapter = Moshi.Builder()
        .add(TestFactory)
        .build()
        .adapter(SealedClassWithExistingLabel::class.java)

    @Test
    fun reading_normal() {
        assertThat(adapter.fromJson("""{"type": "type1"}"""))
            .isEqualTo(SealedClassWithExistingLabel.Type1)
        assertThat(adapter.fromJson("""{"type": "type2", "name": "some name"}"""))
            .isEqualTo(SealedClassWithExistingLabel.Type2("some name"))
    }

    @Test
    fun writing_with_label() {
        assertThat(adapter.toJson(SealedClassWithExistingLabel.Type1))
            .isEqualTo("""{"type":"type1"}""")
        assertThat(adapter.toJson(SealedClassWithExistingLabel.Type2(name = "some name", type = "type2")))
            .isEqualTo("""{"name":"some name","type":"type2"}""")
        assertThat(adapter.toJson(SealedClassWithExistingLabel.Type2(name = "some other name", type = "type3")))
            .isEqualTo("""{"name":"some other name","type":"type3"}""")
    }
}