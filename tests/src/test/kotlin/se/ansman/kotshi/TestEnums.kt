package se.ansman.kotshi

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import org.junit.jupiter.api.Test

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
        assertThat(adapter1.fromJson("\"VALUE1\"")).isEqualTo(SomeEnum.VALUE1)
    }

    @Test
    fun nullValue() {
        assertThat(adapter1.fromJson("null")).isNull()
    }

    @Test
    fun customName() {
        assertThat(adapter1.fromJson("\"VALUE3-alt\"")).isEqualTo(SomeEnum.VALUE3)
        assertThat(adapter1.fromJson("\"VALUE4-alt\"")).isEqualTo(SomeEnum.VALUE4)
    }

    @Test
    fun unknown_error() {
        assertFailure {
            adapter1.fromJson("\"unknown\"")
        }
            .isInstanceOf<JsonDataException>()
    }

    @Test
    fun unknown_fallback() {
        assertThat(adapter2.fromJson("\"VALUE1\"")).isEqualTo(SomeEnumWithFallback.VALUE1)
        assertThat(adapter2.fromJson("\"unknown\"")).isEqualTo(SomeEnumWithFallback.VALUE3)
    }
}