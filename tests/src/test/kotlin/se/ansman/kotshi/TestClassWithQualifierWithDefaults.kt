package se.ansman.kotshi

import com.squareup.moshi.Moshi
import org.junit.jupiter.api.Test

class TestClassWithQualifierWithDefaults {
    private val moshi = Moshi.Builder()
        .add(TestFactory)
        .add { type, annotations, moshi ->
            if (annotations.singleOrNull() is QualifierWithDefaults) {
                moshi.adapter<Any>(type)
            } else {
                null
            }
        }
        .build()

    @Test
    fun test() {
        moshi.adapter(ClassWithQualifierWithDefaults::class.java)
    }
}