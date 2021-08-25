package se.ansman.kotshi

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import org.junit.Test
import kotlin.test.assertEquals

class TestQualifiersWithArguments {
    @Test
    fun test() {
        var callCount = 0
        Moshi.Builder()
            .add(KotshiTestFactory)
            .add { type, annotations, moshi ->
                if (annotations.isEmpty()) {
                    null
                } else when (type) {
                    String::class.java -> {
                        ++callCount
                        val annotation = annotations.single() as QualifierWithDefaults
                        assertThat(annotation.vararg).isEqualTo(arrayOf("vararg"))
                        assertThat(annotation.string).isEqualTo("Hello")
                        moshi.adapter(String::class.java)
                    }
                    Int::class.javaObjectType -> {
                        ++callCount
                        val annotation = annotations.single() as QualifierWithDefaults
                        assertThat(annotation.vararg).isEqualTo(arrayOf("not", "default"))
                        assertThat(annotation.string).isEqualTo("Hello")
                        moshi.adapter(Int::class.java)
                    }
                    Boolean::class.javaObjectType -> {
                        ++callCount
                        val annotation = annotations.single() as QualifierWithDefaults
                        assertThat(annotation.vararg).isEqualTo(arrayOf("vararg"))
                        assertThat(annotation.string).isEqualTo("not default")
                        moshi.adapter(Boolean::class.java)
                    }
                    else -> null
                }
            }
            .build()
            .adapter(ClassWithQualifierWithDefaults::class.java)
        assertEquals(3, callCount)
    }
}