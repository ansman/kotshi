package se.ansman.kotshi

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotEqualTo
import assertk.assertions.isTrue
import com.squareup.moshi.JsonQualifier
import org.junit.jupiter.api.Test
import se.ansman.kotshi.KotshiUtils.createJsonQualifierImplementation
import se.ansman.kotshi.KotshiUtils.matches
import kotlin.reflect.KClass
import kotlin.reflect.javaType
import kotlin.reflect.typeOf

@OptIn(InternalKotshiApi::class, ExperimentalUnsignedTypes::class)
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
            TestQualifier::class.java.createJsonQualifierImplementation(), TestQualifier()
        )

        assertEquality(
            TestQualifier::class.java.createJsonQualifierImplementation(
                mapOf(
                    "vararg" to arrayOf("v1", "v2"),
                    "booleanArg" to true,
                    "byteArg" to 1.toByte(),
                    "ubyteArg" to 2.toByte(),
                    "charArg" to '3',
                    "shortArg" to 4.toShort(),
                    "ushortArg" to 5.toShort(),
                    "intArg" to 6,
                    "uintArg" to 7,
                    "longArg" to 8.toLong(),
                    "ulongArg" to 9.toLong(),
                    "floatArg" to 0.0f,
                    "doubleArg" to Double.NaN,
                    "stringArg" to "value",
                    "emptyArray" to booleanArrayOf(),
                    "booleanArrayArg" to booleanArrayOf(true, false),
                    "byteArrayArg" to byteArrayOf(1, 2, 3),
                    "ubyteArrayArg" to byteArrayOf(1, 2, 3),
                    "charArrayArg" to charArrayOf('a', 'b', 'c'),
                    "shortArrayArg" to shortArrayOf(1, 2, 3),
                    "ushortArrayArg" to shortArrayOf(1, 2, 3),
                    "intArrayArg" to intArrayOf(1, 2, 3),
                    "uintArrayArg" to intArrayOf(1, 2, 3),
                    "longArrayArg" to longArrayOf(1, 2, 3),
                    "ulongArrayArg" to longArrayOf(1, 2, 3),
                    "floatArrayArg" to floatArrayOf(1f, 2f, 3f),
                    "doubleArrayArg" to doubleArrayOf(1.0, 2.0, 3.0),
                    "stringArrayArg" to arrayOf("one", "two", "three"),
                    "classArg" to TestKotshiUtils::class.java,
                    "nestedArg" to TestQualifier.Nested("nested"),
                    "enumArg" to TestEnum.Value2,
                )
            ),
            TestQualifier(
                vararg = arrayOf("v1", "v2"),
                booleanArg = true,
                byteArg = 1,
                ubyteArg = 2u,
                charArg = '3',
                shortArg = 4,
                ushortArg = 5u,
                intArg = 6,
                uintArg = 7u,
                longArg = 8,
                ulongArg = 9u,
                floatArg = 0.0f,
                doubleArg = Double.NaN,
                stringArg = "value",
                emptyArray = booleanArrayOf(),
                booleanArrayArg = booleanArrayOf(true, false),
                byteArrayArg = byteArrayOf(1, 2, 3),
                ubyteArrayArg = ubyteArrayOf(1u, 2u, 3u),
                charArrayArg = charArrayOf('a', 'b', 'c'),
                shortArrayArg = shortArrayOf(1, 2, 3),
                ushortArrayArg = ushortArrayOf(1u, 2u, 3u),
                intArrayArg = intArrayOf(1, 2, 3),
                uintArrayArg = uintArrayOf(1u, 2u, 3u),
                longArrayArg = longArrayOf(1, 2, 3),
                ulongArrayArg = ulongArrayOf(1u, 2u, 3u),
                floatArrayArg = floatArrayOf(1f, 2f, 3f),
                doubleArrayArg = doubleArrayOf(1.0, 2.0, 3.0),
                stringArrayArg = arrayOf("one", "two", "three"),
                classArg = TestKotshiUtils::class,
                nestedArg = TestQualifier.Nested("nested"),
                enumArg = TestEnum.Value2,
            )
        )

        assertEquality(
            TestQualifier::class.java.createJsonQualifierImplementation(),
            TestQualifier::class.java.createJsonQualifierImplementation(mapOf("stringArg" to "string"))
        )

        assertEquality(TestQualifier::class.java.createJsonQualifierImplementation(mapOf("stringArg" to "bar")))
        assertEquality(
            TestQualifier::class.java.createJsonQualifierImplementation(mapOf("stringArg" to "bar")),
            TestQualifier::class.java.createJsonQualifierImplementation(mapOf("stringArg" to "bar"))
        )
        assertEquality(
            TestQualifier::class.java.createJsonQualifierImplementation(mapOf("stringArg" to "bar")),
            TestQualifier(stringArg = "bar")
        )

        assertThat(TestQualifier::class.java.createJsonQualifierImplementation())
            .isNotEqualTo(TestQualifier::class.java.createJsonQualifierImplementation(mapOf("stringArg" to "bar")))

        assertThat(TestQualifier::class.java.createJsonQualifierImplementation(mapOf("stringArg" to "bar")))
            .isNotEqualTo(TestQualifier::class.java.createJsonQualifierImplementation())
    }

    @Test
    @TestQualifier
    fun testCreateJsonQualifierImplementation_hashCode() {
        fun assertEquality(a: TestQualifier, b: TestQualifier = a) {
            assertThat(a.hashCode()).isEqualTo(b.hashCode())
            assertThat(b.hashCode()).isEqualTo(a.hashCode())
        }

        assertEquality(TestQualifier::class.java.createJsonQualifierImplementation())
        assertEquality(
            TestQualifier::class.java.createJsonQualifierImplementation(),
            TestQualifier::class.java.createJsonQualifierImplementation()
        )
        assertEquality(
            TestQualifier::class.java.createJsonQualifierImplementation(),
            TestQualifier()
        )

        assertEquality(
            TestQualifier::class.java.createJsonQualifierImplementation(
                mapOf(
                    "vararg" to arrayOf("v1", "v2"),
                    "booleanArg" to true,
                    "byteArg" to 1.toByte(),
                    "ubyteArg" to 2.toByte(),
                    "charArg" to '3',
                    "shortArg" to 4.toShort(),
                    "ushortArg" to 5.toShort(),
                    "intArg" to 6,
                    "uintArg" to 7,
                    "longArg" to 8.toLong(),
                    "ulongArg" to 9.toLong(),
                    "floatArg" to 0.0f,
                    "doubleArg" to Double.NaN,
                    "stringArg" to "value",
                    "emptyArray" to booleanArrayOf(),
                    "booleanArrayArg" to booleanArrayOf(true, false),
                    "byteArrayArg" to byteArrayOf(1, 2, 3),
                    "ubyteArrayArg" to byteArrayOf(1, 2, 3),
                    "charArrayArg" to charArrayOf('a', 'b', 'c'),
                    "shortArrayArg" to shortArrayOf(1, 2, 3),
                    "ushortArrayArg" to shortArrayOf(1, 2, 3),
                    "intArrayArg" to intArrayOf(1, 2, 3),
                    "uintArrayArg" to intArrayOf(1, 2, 3),
                    "longArrayArg" to longArrayOf(1, 2, 3),
                    "ulongArrayArg" to longArrayOf(1, 2, 3),
                    "floatArrayArg" to floatArrayOf(1f, 2f, 3f),
                    "doubleArrayArg" to doubleArrayOf(1.0, 2.0, 3.0),
                    "stringArrayArg" to arrayOf("one", "two", "three"),
                    "classArg" to TestKotshiUtils::class.java,
                    "nestedArg" to TestQualifier.Nested::class.java.createJsonQualifierImplementation(mapOf("arg" to "nested")),
                    "enumArg" to TestEnum.Value2,
                )
            ),
            TestQualifier(
                vararg = arrayOf("v1", "v2"),
                booleanArg = true,
                byteArg = 1,
                ubyteArg = 2u,
                charArg = '3',
                shortArg = 4,
                ushortArg = 5u,
                intArg = 6,
                uintArg = 7u,
                longArg = 8,
                ulongArg = 9u,
                floatArg = 0.0f,
                doubleArg = Double.NaN,
                stringArg = "value",
                emptyArray = booleanArrayOf(),
                booleanArrayArg = booleanArrayOf(true, false),
                byteArrayArg = byteArrayOf(1, 2, 3),
                ubyteArrayArg = ubyteArrayOf(1u, 2u, 3u),
                charArrayArg = charArrayOf('a', 'b', 'c'),
                shortArrayArg = shortArrayOf(1, 2, 3),
                ushortArrayArg = ushortArrayOf(1u, 2u, 3u),
                intArrayArg = intArrayOf(1, 2, 3),
                uintArrayArg = uintArrayOf(1u, 2u, 3u),
                longArrayArg = longArrayOf(1, 2, 3),
                ulongArrayArg = ulongArrayOf(1u, 2u, 3u),
                floatArrayArg = floatArrayOf(1f, 2f, 3f),
                doubleArrayArg = doubleArrayOf(1.0, 2.0, 3.0),
                stringArrayArg = arrayOf("one", "two", "three"),
                classArg = TestKotshiUtils::class,
                nestedArg = TestQualifier.Nested("nested"),
                enumArg = TestEnum.Value2,
            )
        )

        assertEquality(TestQualifier::class.java.createJsonQualifierImplementation(mapOf("stringArg" to "bar")))
        assertEquality(
            TestQualifier::class.java.createJsonQualifierImplementation(mapOf("stringArg" to "bar")),
            TestQualifier::class.java.createJsonQualifierImplementation(mapOf("stringArg" to "bar"))
        )
        assertEquality(
            TestQualifier::class.java.createJsonQualifierImplementation(mapOf("stringArg" to "bar")),
            TestQualifier(stringArg = "bar")
        )

        assertThat(TestQualifier::class.java.createJsonQualifierImplementation().hashCode())
            .isNotEqualTo(
                TestQualifier::class.java.createJsonQualifierImplementation(mapOf("stringArg" to "bar")).hashCode()
            )

        assertThat(TestQualifier::class.java.createJsonQualifierImplementation(mapOf("stringArg" to "bar")).hashCode())
            .isNotEqualTo(TestQualifier::class.java.createJsonQualifierImplementation().hashCode())
    }

    @Test
    fun testCreateJsonQualifierImplementation_toString() {
        assertThat(
            TestQualifier::class.java.createJsonQualifierImplementation(
                mapOf("nestedArg" to TestQualifier.Nested::class.java.createJsonQualifierImplementation(mapOf("arg" to "string")))
            ).toString()
        )
            .isEqualTo(
                "@se.ansman.kotshi.TestQualifier(" +
                    listOf(
                        "booleanArg=false",
                        "booleanArrayArg=[]",
                        "byteArg=0",
                        "byteArrayArg=[]",
                        "charArg=\u0000",
                        "charArrayArg=[]",
                        "classArg=interface se.ansman.kotshi.TestQualifier",
                        "doubleArg=0.0",
                        "doubleArrayArg=[]",
                        "emptyArray=[]",
                        "enumArg=Value1",
                        "floatArg=0.0",
                        "floatArrayArg=[]",
                        "intArg=0",
                        "intArrayArg=[]",
                        "longArg=0",
                        "longArrayArg=[]",
                        "nestedArg=@se.ansman.kotshi.TestQualifier\$Nested(arg=string)",
                        "shortArg=0",
                        "shortArrayArg=[]",
                        "stringArg=string",
                        "stringArrayArg=[]",
                        "ubyteArg=0",
                        "ubyteArrayArg=[]",
                        "uintArg=0",
                        "uintArrayArg=[]",
                        "ulongArg=0",
                        "ulongArrayArg=[]",
                        "ushortArg=0",
                        "ushortArrayArg=[]",
                        "vararg=[]"
                    ).joinToString(", ") + ")"
            )

        assertThat(
            TestQualifier::class.java.createJsonQualifierImplementation(
                mapOf(
                    "vararg" to arrayOf("v1", "v2"),
                    "booleanArg" to true,
                    "byteArg" to 1.toByte(),
                    "ubyteArg" to 2.toByte(),
                    "charArg" to '3',
                    "shortArg" to 4.toShort(),
                    "ushortArg" to 5.toShort(),
                    "intArg" to 6,
                    "uintArg" to 7,
                    "longArg" to 8.toLong(),
                    "ulongArg" to 9.toLong(),
                    "floatArg" to 0.0f,
                    "doubleArg" to Double.NaN,
                    "stringArg" to "value",
                    "emptyArray" to booleanArrayOf(),
                    "booleanArrayArg" to booleanArrayOf(true, false),
                    "byteArrayArg" to byteArrayOf(1, 2, 3),
                    "ubyteArrayArg" to byteArrayOf(1, 2, 3),
                    "charArrayArg" to charArrayOf('a', 'b', 'c'),
                    "shortArrayArg" to shortArrayOf(1, 2, 3),
                    "ushortArrayArg" to shortArrayOf(1, 2, 3),
                    "intArrayArg" to intArrayOf(1, 2, 3),
                    "uintArrayArg" to intArrayOf(1, 2, 3),
                    "longArrayArg" to longArrayOf(1, 2, 3),
                    "ulongArrayArg" to longArrayOf(1, 2, 3),
                    "floatArrayArg" to floatArrayOf(1f, 2f, 3f),
                    "doubleArrayArg" to doubleArrayOf(1.0, 2.0, 3.0),
                    "stringArrayArg" to arrayOf("one", "two", "three"),
                    "classArg" to TestKotshiUtils::class.java,
                    "nestedArg" to TestQualifier.Nested::class.java.createJsonQualifierImplementation(mapOf("arg" to "nested")),
                    "enumArg" to TestEnum.Value2,
                )
            ).toString()
        )
            .isEqualTo(
                "@se.ansman.kotshi.TestQualifier(" + listOf(
                    "booleanArg=true",
                    "booleanArrayArg=[true, false]",
                    "byteArg=1",
                    "byteArrayArg=[1, 2, 3]",
                    "charArg=3",
                    "charArrayArg=[a, b, c]",
                    "classArg=class se.ansman.kotshi.TestKotshiUtils",
                    "doubleArg=NaN",
                    "doubleArrayArg=[1.0, 2.0, 3.0]",
                    "emptyArray=[]",
                    "enumArg=Value2",
                    "floatArg=0.0",
                    "floatArrayArg=[1.0, 2.0, 3.0]",
                    "intArg=6",
                    "intArrayArg=[1, 2, 3]",
                    "longArg=8",
                    "longArrayArg=[1, 2, 3]",
                    "nestedArg=@se.ansman.kotshi.TestQualifier\$Nested(arg=nested)",
                    "shortArg=4",
                    "shortArrayArg=[1, 2, 3]",
                    "stringArg=value",
                    "stringArrayArg=[one, two, three]",
                    "ubyteArg=2",
                    "ubyteArrayArg=[1, 2, 3]",
                    "uintArg=7",
                    "uintArrayArg=[1, 2, 3]",
                    "ulongArg=9",
                    "ulongArrayArg=[1, 2, 3]",
                    "ushortArg=5",
                    "ushortArrayArg=[1, 2, 3]",
                    "vararg=[v1, v2]"
                ).joinToString(", ") + ")"
            )
    }

    @Test
    fun testCreateJsonQualifierImplementation_annotationType() {
        assertThat(TestQualifier::class.java.createJsonQualifierImplementation().annotationClass)
            .isEqualTo(TestQualifier::class)
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testMatches() {
        assertThat(
            matches(
                String::class.java,
                String::class.java
            )
        ).isTrue()

        assertThat(
            matches(
                Int::class.java,
                String::class.java
            )
        ).isFalse()

        assertThat(
            matches(
                String::class.java,
                Int::class.java
            )
        ).isFalse()

        assertThat(
            matches(
                typeOf<MutableList<String>>().javaType,
                typeOf<MutableList<String>>().javaType
            )
        ).isTrue()

        assertThat(
            matches(
                typeOf<MutableList<out String>>().javaType,
                typeOf<MutableList<String>>().javaType
            )
        ).isTrue()

        assertThat(
            matches(
                typeOf<MutableList<out String>>().javaType,
                typeOf<MutableList<Int>>().javaType
            )
        ).isFalse()

        assertThat(
            matches(
                typeOf<MutableList<out CharSequence>>().javaType,
                typeOf<MutableList<String>>().javaType
            )
        ).isTrue()

        assertThat(
            matches(
                typeOf<MutableList<in String>>().javaType,
                typeOf<MutableList<CharSequence>>().javaType
            )
        ).isTrue()

        assertThat(
            matches(
                typeOf<MutableList<in CharSequence>>().javaType,
                typeOf<MutableList<String>>().javaType
            )
        ).isFalse()

        assertThat(
            matches(
                typeOf<MutableList<*>>().javaType,
                typeOf<MutableList<String>>().javaType
            )
        ).isTrue()
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
@Suppress("unused")
@JsonQualifier
annotation class TestQualifier(
    vararg val vararg: String = [],
    val booleanArg: Boolean = false,
    val byteArg: Byte = 0,
    val ubyteArg: UByte = 0u,
    val charArg: Char = '\u0000',
    val shortArg: Short = 0,
    val ushortArg: UShort = 0u,
    val intArg: Int = 0,
    val uintArg: UInt = 0u,
    val longArg: Long = 0,
    val ulongArg: ULong = 0u,
    val floatArg: Float = 0f,
    val doubleArg: Double = 0.0,
    val stringArg: String = "string",
    val emptyArray: BooleanArray = [],
    val booleanArrayArg: BooleanArray = [],
    val byteArrayArg: ByteArray = [],
    val ubyteArrayArg: UByteArray = [],
    val charArrayArg: CharArray = [],
    val shortArrayArg: ShortArray = [],
    val ushortArrayArg: UShortArray = [],
    val intArrayArg: IntArray = [],
    val uintArrayArg: UIntArray = [],
    val longArrayArg: LongArray = [],
    val ulongArrayArg: ULongArray = [],
    val floatArrayArg: FloatArray = [],
    val doubleArrayArg: DoubleArray = [],
    val stringArrayArg: Array<String> = [],
    val classArg: KClass<*> = TestQualifier::class,
    val nestedArg: Nested = Nested("string"),
    val enumArg: TestEnum = TestEnum.Value1,
) {
    annotation class Nested(val arg: String)
}

enum class TestEnum {
    Value1,
    Value2,
}