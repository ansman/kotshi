package se.ansman.kotshi

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.squareup.moshi.Moshi
import org.junit.jupiter.api.Test
import se.ansman.kotshi.assertions.isEmpty

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
//                        assertThat(annotation.ubyteArrayArg).all {
//                            isInstanceOf(UByteArray::class.java)
//                            isEmpty()
//                        }
                        assertThat(annotation.charArrayArg).isEmpty()
                        assertThat(annotation.shortArrayArg).isEmpty()
//                        assertThat(annotation.ushortArrayArg).all {
//                            isInstanceOf(UShortArray::class.java)
//                            isEmpty()
//                        }
                        assertThat(annotation.intArrayArg).isEmpty()
//                        assertThat(annotation.uintArrayArg).all {
//                            isInstanceOf(UIntArray::class.java)
//                            isEmpty()
//                        }
                        assertThat(annotation.longArrayArg).isEmpty()
//                        assertThat(annotation.ulongArrayArg).all {
//                            isInstanceOf(ULongArray::class.java)
//                            isEmpty()
//                        }
                        assertThat(annotation.floatArrayArg).isEmpty()
                        assertThat(annotation.doubleArrayArg).isEmpty()
                        assertThat(annotation.stringArrayArg).isEmpty()
                        assertThat(annotation.enumArrayArg).isEmpty()
                        assertThat(annotation.annotationArrayArg).isEmpty()
                        assertThat(annotation.classArrayArg).isEmpty()
                        moshi.adapter(String::class.java)
                    }

                    else -> null
                }
            }
            .build()
            .adapter(ClassWithQualifierWithEmptyArrays::class.java)
        assertThat(1)
            .isEqualTo(callCount)
    }
}