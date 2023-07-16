package se.ansman.kotshi

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.squareup.moshi.Moshi
import org.junit.jupiter.api.Test

class TestSerializeNulls {
    private val adapter = Moshi.Builder()
        .add(TestFactory)
        .build()
        .adapter(ClassWithSerializeNulls::class.java)

    @Test
    fun root() {
        assertThat(
            adapter.toJson(ClassWithSerializeNulls(ClassWithSerializeNulls.Nested("v")))
        ).isEqualTo<String?>("""{"nested":{"value":"v"}}""")
        assertThat(adapter.toJson(ClassWithSerializeNulls(null)))
            .isEqualTo("""{"nested":null}""")
    }

    @Test
    fun nested() {
        assertThat(adapter.toJson(ClassWithSerializeNulls(ClassWithSerializeNulls.Nested(null))))
            .isEqualTo<String?>("""{"nested":{"value":null}}""")
    }
}