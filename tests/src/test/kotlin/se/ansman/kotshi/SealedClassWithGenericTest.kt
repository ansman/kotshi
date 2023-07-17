package se.ansman.kotshi

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.junit.jupiter.api.Test

class SealedClassWithGenericTest {
    private val adapter = Moshi.Builder()
        .add(TestFactory)
        .build()
        .adapter<SealedClassWithGeneric<String>>(
            Types.newParameterizedType(
                SealedClassWithGeneric::class.java,
                String::class.java
            )
        )

    @Test
    fun testFromJson() {
        assertThat(adapter.fromJson("""{"type":"success","data":"hello"}"""))
            .isEqualTo(SealedClassWithGeneric.Success("hello"))

        assertThat(adapter.fromJson("""{"type":"error","error":"Something went wrong"}"""))
            .isEqualTo(SealedClassWithGeneric.Error("Something went wrong"))
    }

    @Test
    fun testToJson() {
        assertThat(adapter.toJson(SealedClassWithGeneric.Success("hello")))
            .isEqualTo("""{"type":"success","data":"hello"}""")

        assertThat(adapter.toJson(SealedClassWithGeneric.Error("Something went wrong")))
            .isEqualTo("""{"type":"error","error":"Something went wrong"}""")
    }
}