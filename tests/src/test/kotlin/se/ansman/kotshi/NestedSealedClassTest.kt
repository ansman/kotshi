package se.ansman.kotshi

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.squareup.moshi.Moshi
import org.junit.jupiter.api.Test

class NestedSealedClassTest {
    private val adapter = Moshi.Builder()
        .add(TestFactory)
        .build()
        .adapter(NestedSealedClass::class.java)

    @Test
    fun nested1() {
        assertThat(adapter.fromJson("""{"type":"nestedChild1","v":"nc1"}"""))
            .isEqualTo(NestedSealedClass.Nested1.NestedChild1("nc1"))
        assertThat(adapter.toJson(NestedSealedClass.Nested1.NestedChild1("nc1")))
            .isEqualTo("""{"type":"nestedChild1","v":"nc1"}""")
    }

    @Test
    fun nested2() {
        assertThat(adapter.fromJson("""{"type":"nestedChild2","v":"nc2"}"""))
            .isEqualTo(NestedSealedClass.Nested1.Nested2.NestedChild2("nc2"))
        assertThat(adapter.toJson(NestedSealedClass.Nested1.NestedChild1("nc2")))
            .isEqualTo("""{"type":"nestedChild1","v":"nc2"}""")
    }

    @Test
    fun nested3() {
        assertThat(adapter.fromJson("""{"type":"nested3","subtype":"nestedChild4","v":"nc4"}"""))
            .isEqualTo(NestedSealedClass.Nested3.NestedChild4("nc4"))
        assertThat(adapter.toJson(NestedSealedClass.Nested3.NestedChild4("nc4")))
            .isEqualTo("""{"type":"nested3","subtype":"nestedChild4","v":"nc4"}""")
    }
}