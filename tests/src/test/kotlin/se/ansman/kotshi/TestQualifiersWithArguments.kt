package se.ansman.kotshi

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.squareup.moshi.Moshi
import org.junit.jupiter.api.Test
import se.ansman.kotshi.assertions.hasContentsEqualTo

class TestQualifiersWithArguments {
    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun test() {
        var callCount = 0
        Moshi.Builder()
            .add(TestFactory)
            .add { type, annotations, moshi ->
                if (type == String::class.java && annotations.isNotEmpty()) {
                    ++callCount
                    val annotation = annotations.single() as QualifierWithArguments
                    assertThat(arrayOf("vararg")).hasContentsEqualTo(annotation.vararg)
                    assertThat(annotation.booleanArg).isEqualTo(true)
                    assertThat(annotation.byteArg).isEqualTo(124)
//                    assertThat(annotation.ubyteArg).isEqualTo(254u)
                    assertThat(annotation.shortArg).isEqualTo(10_000)
//                    assertThat(annotation.ushortArg).isEqualTo(32_768u)
                    assertThat(annotation.charArg).isEqualTo('K')
                    assertThat(annotation.intArg).isEqualTo(100_000)
//                    assertThat(annotation.uintArg).isEqualTo(2147483648u)
                    assertThat(annotation.longArg).isEqualTo(100_000_000_000_000)
//                    assertThat(annotation.ulongArg).isEqualTo(9_223_372_036_854_775_808u)
                    assertThat(annotation.floatArg).isEqualTo(1f)
                    assertThat(annotation.doubleArg).isEqualTo(2.0)
                    assertThat(annotation.stringArg).isEqualTo<String>("string")
                    assertThat(booleanArrayOf()).hasContentsEqualTo(annotation.emptyArray)
                    assertThat(booleanArrayOf(true)).hasContentsEqualTo(annotation.booleanArrayArg)
                    assertThat(byteArrayOf(124)).hasContentsEqualTo(annotation.byteArrayArg)
//                    assertThat(ubyteArrayOf(254u)).hasContentsEqualTo(annotation.ubyteArrayArg)
                    assertThat(shortArrayOf(10_000)).hasContentsEqualTo(annotation.shortArrayArg)
//                    assertThat(ushortArrayOf(32_768u)).hasContentsEqualTo(annotation.ushortArrayArg)
                    assertThat(charArrayOf('K')).hasContentsEqualTo(annotation.charArrayArg)
                    assertThat(intArrayOf(100_000)).hasContentsEqualTo(annotation.intArrayArg)
//                    assertThat(uintArrayOf(2147483648u)).hasContentsEqualTo(annotation.uintArrayArg)
                    assertThat(longArrayOf(100_000_000_000_000)).hasContentsEqualTo(annotation.longArrayArg)
//                    assertThat(ulongArrayOf(9_223_372_036_854_775_808u)).hasContentsEqualTo(annotation.ulongArrayArg)
                    assertThat(floatArrayOf(47.11f)).hasContentsEqualTo(annotation.floatArrayArg)
                    assertThat(doubleArrayOf(13.37)).hasContentsEqualTo(annotation.doubleArrayArg)
                    assertThat(arrayOf("string")).hasContentsEqualTo(annotation.stringArrayArg)
                    assertThat(annotation.classArg).isEqualTo(QualifierWithArguments::class)
                    assertThat(annotation.nestedArg.arg).isEqualTo<String>("nested")
                    assertThat(annotation.enumArg).isEqualTo(SomeEnum.VALUE3)
                    moshi.adapter(String::class.java)
                } else {
                    null
                }
            }
            .build()
            .adapter(ClassWithQualifierWithArguments::class.java)
        assertThat(callCount).isEqualTo(1)
    }
}