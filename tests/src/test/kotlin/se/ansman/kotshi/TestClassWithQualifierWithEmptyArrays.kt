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
                        assertThat(annotation.booleanArrayArg).isEmpty()
                        assertThat(annotation.byteArrayArg).isEmpty()
                        assertThat(annotation.ubyteArrayArg).apply {
                            isInstanceOf(UByteArray::class.java)
                            isEmpty()
                        }
                        assertThat(annotation.charArrayArg).isEmpty()
                        assertThat(annotation.shortArrayArg).isEmpty()
                        assertThat(annotation.ushortArrayArg).apply {
                            isInstanceOf(UShortArray::class.java)
                            isEmpty()
                        }
                        assertThat(annotation.intArrayArg).isEmpty()
                        assertThat(annotation.uintArrayArg).apply {
                            isInstanceOf(UIntArray::class.java)
                            isEmpty()
                        }
                        assertThat(annotation.longArrayArg).isEmpty()
                        assertThat(annotation.ulongArrayArg).apply {
                            isInstanceOf(ULongArray::class.java)
                            isEmpty()
                        }
                        assertThat(annotation.floatArrayArg).isEmpty()
                        assertThat(annotation.doubleArrayArg).isEmpty()
                        assertThat(annotation.stringArrayArg).isEmpty()
                        assertThat(annotation.enumArrayArg).isEmpty()
                        assertThat(annotation.annotationArrayArg).isEmpty()
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