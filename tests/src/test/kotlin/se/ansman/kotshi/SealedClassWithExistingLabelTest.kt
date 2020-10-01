package se.ansman.kotshi

import com.squareup.moshi.Moshi
import org.junit.Test
import kotlin.test.assertEquals

class SealedClassWithExistingLabelTest {
    private val adapter = Moshi.Builder()
        .add(TestFactory)
        .build()
        .adapter(SealedClassWithExistingLabel::class.java)

    @Test
    fun reading_normal() {
        assertEquals(SealedClassWithExistingLabel.Type1, adapter.fromJson("""{"type": "type1"}"""))
        assertEquals(
            SealedClassWithExistingLabel.Type2("some name"),
            adapter.fromJson("""{"type": "type2", "name": "some name"}""")
        )
    }

    @Test
    fun writing_with_label() {
        assertEquals("""{"type":"type1"}""", adapter.toJson(SealedClassWithExistingLabel.Type1))
        assertEquals(
            """{"name":"some name","type":"type2"}""",
            adapter.toJson(SealedClassWithExistingLabel.Type2(name = "some name", type = "type2"))
        )
        assertEquals(
            """{"name":"some other name","type":"type3"}""",
            adapter.toJson(SealedClassWithExistingLabel.Type2(name = "some other name", type = "type3"))
        )
    }
}