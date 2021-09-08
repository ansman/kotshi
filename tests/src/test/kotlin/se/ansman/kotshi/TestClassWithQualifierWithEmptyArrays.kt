package se.ansman.kotshi

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import org.junit.Test
import kotlin.test.assertEquals

class TestClassWithQualifierWithEmptyArrays {
    @OptIn(ExperimentalUnsignedTypes::class)
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
                        val annotation = annotations.single() as QualifierWithArrays
                        assertThat(annotation.booleanArrayArg).isEqualTo(booleanArrayOf())
                        assertThat(annotation.byteArrayArg).isEqualTo(byteArrayOf())
                        assertThat(annotation.ubyteArrayArg).apply {
                            isInstanceOf(UByteArray::class.java)
                            isEmpty()
                        }
                        assertThat(annotation.charArrayArg).isEqualTo(charArrayOf())
                        assertThat(annotation.shortArrayArg).isEqualTo(shortArrayOf())
                        assertThat(annotation.ushortArrayArg).apply {
                            isInstanceOf(UShortArray::class.java)
                            isEmpty()
                        }
                        assertThat(annotation.intArrayArg).isEqualTo(intArrayOf())
                        assertThat(annotation.uintArrayArg).apply {
                            isInstanceOf(UIntArray::class.java)
                            isEmpty()
                        }
                        assertThat(annotation.longArrayArg).isEqualTo(longArrayOf())
                        assertThat(annotation.ulongArrayArg).apply {
                            isInstanceOf(ULongArray::class.java)
                            isEmpty()
                        }
                        @Suppress("DEPRECATION")
                        assertThat(annotation.floatArrayArg).isEqualTo(floatArrayOf(), 0.0f)
                        @Suppress("DEPRECATION")
                        assertThat(annotation.doubleArrayArg).isEqualTo(doubleArrayOf(), 0.0)
                        assertThat(annotation.stringArrayArg).isEqualTo(arrayOf<String>())
                        assertThat(annotation.enumArrayArg).isEqualTo(emptyArray<Polymorphic.Fallback>())
                        assertThat(annotation.annotationArrayArg).isEqualTo(emptyArray<Polymorphic>())
                        // This will fail with runtime annotations https://youtrack.jetbrains.com/issue/KT-47703 until fixed
                        // assertThat(annotation.classArrayArg).isEqualTo(emptyArray<KClass<*>>())
                        moshi.adapter(String::class.java)
                    }
                    else -> null
                }
            }
            .build()
            .adapter(ClassWithQualifierWithEmptyArrays::class.java)
        assertEquals(1, callCount)
    }
}