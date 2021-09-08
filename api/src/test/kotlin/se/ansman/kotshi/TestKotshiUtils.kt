package se.ansman.kotshi

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.JsonQualifier
import org.junit.Test
import se.ansman.kotshi.KotshiUtils.createJsonQualifierImplementation
import se.ansman.kotshi.KotshiUtils.matches
import kotlin.reflect.javaType
import kotlin.reflect.typeOf
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(InternalKotshiApi::class)
@Suppress("DEPRECATION_ERROR")
class TestKotshiUtils {
    @Test
    fun testCreateJsonQualifierImplementation_equals() {
        fun assertEquality(a: TestQualifier, b: TestQualifier = a) {
            assertThat(a).isEqualTo(b)
            assertThat(b).isEqualTo(a)
        }

        assertEquality(TestQualifier::class.java.createJsonQualifierImplementation())
        assertEquality(
            TestQualifier::class.java.createJsonQualifierImplementation(),
            TestQualifier::class.java.createJsonQualifierImplementation()
        )
        assertEquality(
            TestQualifier::class.java.createJsonQualifierImplementation(),
            TestQualifier::class.java.createJsonQualifierImplementation(mapOf("foo" to "n/a"))
        )

        assertEquality(TestQualifier::class.java.createJsonQualifierImplementation(mapOf("foo" to "bar")))
        assertEquality(
            TestQualifier::class.java.createJsonQualifierImplementation(mapOf("foo" to "bar")),
            TestQualifier::class.java.createJsonQualifierImplementation(mapOf("foo" to "bar"))
        )

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
        assertEquality(
            TestQualifier::class.java.createJsonQualifierImplementation(mapOf("foo" to "bar")),
            TestQualifier::class.java.createJsonQualifierImplementation(mapOf("foo" to "bar"))
        )

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

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testMatches() {
        assertTrue(
            matches(
                String::class.java,
                String::class.java
            )
        )

        assertFalse(
            matches(
                Int::class.java,
                String::class.java
            )
        )

        assertFalse(
            matches(
                String::class.java,
                Int::class.java
            )
        )

        assertTrue(
            matches(
                typeOf<MutableList<String>>().javaType,
                typeOf<MutableList<String>>().javaType
            )
        )

        assertTrue(
            matches(
                typeOf<MutableList<out String>>().javaType,
                typeOf<MutableList<String>>().javaType
            )
        )

        assertFalse(
            matches(
                typeOf<MutableList<out String>>().javaType,
                typeOf<MutableList<Int>>().javaType
            )
        )

        assertTrue(
            matches(
                typeOf<MutableList<out CharSequence>>().javaType,
                typeOf<MutableList<String>>().javaType
            )
        )

        assertTrue(
            matches(
                typeOf<MutableList<in String>>().javaType,
                typeOf<MutableList<CharSequence>>().javaType
            )
        )

        assertFalse(
            matches(
                typeOf<MutableList<in CharSequence>>().javaType,
                typeOf<MutableList<String>>().javaType
            )
        )

        assertTrue(
            matches(
                typeOf<MutableList<*>>().javaType,
                typeOf<MutableList<String>>().javaType
            )
        )
    }
}

@JsonQualifier
annotation class TestQualifier(val foo: String = "n/a")