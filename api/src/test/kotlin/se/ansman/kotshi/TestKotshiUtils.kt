package se.ansman.kotshi

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.JsonQualifier
import org.junit.Test
import se.ansman.kotshi.KotshiUtils.createJsonQualifierImplementation

@OptIn(InternalKotshiApi::class)
class TestKotshiUtils {

    @Test
    fun testCreateJsonQualifierImplementation_equals() {
        fun assertEquality(a: TestQualifier, b: TestQualifier = a) {
            assertThat(a).isEqualTo(b)
            assertThat(b).isEqualTo(a)
        }

        assertEquality(TestQualifier::class.java.createJsonQualifierImplementation())
        assertEquality(TestQualifier::class.java.createJsonQualifierImplementation(), TestQualifier::class.java.createJsonQualifierImplementation())
        assertEquality(TestQualifier::class.java.createJsonQualifierImplementation(), TestQualifier::class.java.createJsonQualifierImplementation(mapOf("foo" to "n/a")))

        assertEquality(TestQualifier::class.java.createJsonQualifierImplementation(mapOf("foo" to "bar")))
        assertEquality(TestQualifier::class.java.createJsonQualifierImplementation(mapOf("foo" to "bar")), TestQualifier::class.java.createJsonQualifierImplementation(mapOf("foo" to "bar")))

        assertThat(TestQualifier::class.java.createJsonQualifierImplementation())
            .isNotEqualTo(TestQualifier::class.java.createJsonQualifierImplementation(mapOf("foo" to "bar")))
        assertThat(TestQualifier::class.java.createJsonQualifierImplementation(mapOf("foo" to "bar")))
            .isNotEqualTo(TestQualifier::class.java.createJsonQualifierImplementation())
    }

    @Test
    fun testCreateJsonQualifierImplementation_hashCode() {
        fun assertEquality(a: TestQualifier, b: TestQualifier = a) {
            assertThat(a.hashCode()).isEqualTo(b.hashCode())
            assertThat(b.hashCode()).isEqualTo(a.hashCode())
        }

        assertEquality(TestQualifier::class.java.createJsonQualifierImplementation(mapOf("foo" to "bar")))
        assertEquality(TestQualifier::class.java.createJsonQualifierImplementation(mapOf("foo" to "bar")), TestQualifier::class.java.createJsonQualifierImplementation(mapOf("foo" to "bar")))

        assertThat(TestQualifier::class.java.createJsonQualifierImplementation().hashCode())
            .isNotEqualTo(TestQualifier::class.java.createJsonQualifierImplementation(mapOf("foo" to "bar")).hashCode())
        assertThat(TestQualifier::class.java.createJsonQualifierImplementation(mapOf("foo" to "bar")).hashCode())
            .isNotEqualTo(TestQualifier::class.java.createJsonQualifierImplementation().hashCode())
    }

    @Test
    fun testCreateJsonQualifierImplementation_toString() {
        assertThat(TestQualifier::class.java.createJsonQualifierImplementation().toString())
            .isEqualTo("@se.ansman.kotshi.TestQualifier()")

        assertThat(TestQualifier::class.java.createJsonQualifierImplementation(mapOf("foo" to "bar")).toString())
            .isEqualTo("@se.ansman.kotshi.TestQualifier(foo=bar)")
    }
}

@JsonQualifier
annotation class TestQualifier(val foo: String = "n/a")