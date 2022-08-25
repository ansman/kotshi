package se.ansman.kotshi

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TestEnums {
    private val adapter1: JsonAdapter<SomeEnum>
    private val adapter2: JsonAdapter<SomeEnumWithFallback>

    init {
        val moshi = Moshi.Builder()
            .add(TestFactory)
            .build()
        adapter1 = moshi.adapter(SomeEnum::class.java)
        adapter2 = moshi.adapter(SomeEnumWithFallback::class.java)
    }

    @Test
    fun normal() {
        assertEquals(SomeEnum.VALUE1, adapter1.fromJson("\"VALUE1\""))
    }

    @Test
    fun nullValue() {
        assertEquals(null, adapter1.fromJson("null"))
    }

    @Test
    fun customName() {
        assertEquals(SomeEnum.VALUE3, adapter1.fromJson("\"VALUE3-alt\""))
        assertEquals(SomeEnum.VALUE4, adapter1.fromJson("\"VALUE4-alt\""))
    }

    @Test
    fun unknown_error() {
        assertFailsWith<JsonDataException> {
            adapter1.fromJson("\"unknown\"")
        }
    }

    @Test
    fun unknown_fallback() {
        assertEquals(SomeEnumWithFallback.VALUE1, adapter2.fromJson("\"VALUE1\""))
        assertEquals(SomeEnumWithFallback.VALUE3, adapter2.fromJson("\"unknown\""))
    }
}