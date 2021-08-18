package se.ansman.kotshi

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.JsonQualifier
import org.junit.Test
import se.ansman.kotshi.KotshiUtils.createJsonQualifierImplementation

@OptIn(InternalKotshiApi::class)
class TestKotshiUtils {
    private val noArgs = TestQualifier::class.java.createJsonQualifierImplementation()
    private val withArgs = TestQualifier::class.java.createJsonQualifierImplementation(mapOf("foo" to "bar"))

    @Test
    fun testCreateJsonQualifierImplementation_equals() {
        assertThat(noArgs).isEqualTo(noArgs)
        assertThat(noArgs).isEqualTo(TestQualifier::class.java.createJsonQualifierImplementation())
        assertThat(withArgs).isEqualTo(withArgs)
        assertThat(withArgs).isEqualTo(TestQualifier::class.java.createJsonQualifierImplementation(mapOf("foo" to "bar")))
        assertThat(withArgs).isNotEqualTo(TestQualifier::class.java.createJsonQualifierImplementation(mapOf("foo" to "baz")))
        assertThat(noArgs).isNotEqualTo(withArgs)
        assertThat(withArgs).isNotEqualTo(noArgs)
    }

    @Test
    fun testCreateJsonQualifierImplementation_hashCode() {
        assertThat(noArgs.hashCode()).isEqualTo(noArgs.hashCode())
        assertThat(noArgs.hashCode()).isEqualTo(TestQualifier::class.java.createJsonQualifierImplementation().hashCode())
        assertThat(withArgs.hashCode()).isEqualTo(withArgs.hashCode())
        assertThat(withArgs.hashCode()).isEqualTo(TestQualifier::class.java.createJsonQualifierImplementation(mapOf("foo" to "bar")).hashCode())
        assertThat(withArgs.hashCode()).isNotEqualTo(TestQualifier::class.java.createJsonQualifierImplementation(mapOf("foo" to "baz")).hashCode())
        assertThat(noArgs.hashCode()).isNotEqualTo(withArgs.hashCode())
        assertThat(withArgs.hashCode()).isNotEqualTo(noArgs.hashCode())
    }

    @Test
    fun testCreateJsonQualifierImplementation_toString() {
        assertThat(noArgs.toString()).isEqualTo("@se.ansman.kotshi.TestQualifier()")
        assertThat(withArgs.toString()).isEqualTo("@se.ansman.kotshi.TestQualifier(foo=bar)")
    }
}

@JsonQualifier
private annotation class TestQualifier(val foo: String = "n/a")