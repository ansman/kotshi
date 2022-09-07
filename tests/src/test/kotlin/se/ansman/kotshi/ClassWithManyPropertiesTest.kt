package se.ansman.kotshi

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import org.junit.Test

class ClassWithManyPropertiesTest {
    private val adapter = Moshi.Builder()
        .add(TestFactory)
        .build()
        .adapter(ClassWithManyProperties::class.java)

    @Test
    fun canReadJson() {
        assertThat(adapter.fromJson("{}"))
            .isEqualTo(ClassWithManyProperties())
    }
}