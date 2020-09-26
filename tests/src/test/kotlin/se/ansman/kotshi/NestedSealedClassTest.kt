package se.ansman.kotshi

import com.squareup.moshi.Moshi
import kotlin.test.Test
import kotlin.test.assertEquals

class NestedSealedClassTest {
    private val adapter = Moshi.Builder()
        .add(TestFactory)
        .build()
        .adapter(NestedSealedClass::class.java)

    @Test
    fun nested1() {
        assertEquals(NestedSealedClass.Nested1.NestedChild1("nc1"), adapter.fromJson("""{"type":"nestedChild1","v":"nc1"}"""))
        assertEquals("""{"type":"nestedChild1","v":"nc1"}""", adapter.toJson(NestedSealedClass.Nested1.NestedChild1("nc1")))
    }

    @Test
    fun nested2() {
        assertEquals(NestedSealedClass.Nested1.Nested2.NestedChild2("nc2"), adapter.fromJson("""{"type":"nestedChild2","v":"nc2"}"""))
        assertEquals("""{"type":"nestedChild1","v":"nc2"}""", adapter.toJson(NestedSealedClass.Nested1.NestedChild1("nc2")))
    }

    @Test
    fun nested3() {
        assertEquals(NestedSealedClass.Nested3.NestedChild4("nc4"), adapter.fromJson("""{"type":"nested3","subtype":"nestedChild4","v":"nc4"}"""))
        assertEquals("""{"subtype":"nestedChild4","v":"nc4"}""", adapter.toJson(NestedSealedClass.Nested3.NestedChild4("nc4")))
    }
}