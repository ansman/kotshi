package se.ansman.kotshi

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import com.squareup.moshi.Moshi
import org.junit.jupiter.api.Test

class TestQualifiersWithDefaults {
    @Test
    fun test() {
        var callCount = 0
        Moshi.Builder()
            .add(TestFactory)
            .add { type, annotations, moshi ->
                if (annotations.isEmpty()) {
                    null
                } else when (type) {
                    String::class.java -> {
                        ++callCount
                        val annotation = annotations.single() as QualifierWithDefaults
                        assertThat(annotation.vararg).containsExactly("vararg")
                        assertThat(annotation.string).isEqualTo("Hello")
                        moshi.adapter(String::class.java)
                    }

                    Int::class.javaObjectType -> {
                        ++callCount
                        val annotation = annotations.single() as QualifierWithDefaults
                        assertThat(annotation.vararg).containsExactly("not", "default")
                        assertThat(annotation.string).isEqualTo("Hello")
                        moshi.adapter(Int::class.java)
                    }

                    Boolean::class.javaObjectType -> {
                        ++callCount
                        val annotation = annotations.single() as QualifierWithDefaults
                        assertThat(annotation.vararg).containsExactly("vararg")
                        assertThat(annotation.string).isEqualTo("not default")
                        moshi.adapter(Boolean::class.java)
                    }

                    else -> null
                }
            }
            .build()
            .adapter(ClassWithQualifierWithDefaults::class.java)
        assertThat(callCount).isEqualTo(3)
    }
}