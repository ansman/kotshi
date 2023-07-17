package se.ansman.kotshi

import assertk.assertThat
import assertk.assertions.isDataClassEqualTo
import assertk.assertions.isNotNull
import com.squareup.moshi.Moshi
import org.junit.jupiter.api.Test

class ClassWithManyPropertiesTest {
    private val adapter = Moshi.Builder()
        .add(TestFactory)
        .build()
        .adapter(ClassWithManyProperties::class.java)

    @Test
    fun canReadJson() {
        assertThat(adapter.fromJson("{}"))
            .isNotNull()
            .isDataClassEqualTo(ClassWithManyProperties())
    }
}