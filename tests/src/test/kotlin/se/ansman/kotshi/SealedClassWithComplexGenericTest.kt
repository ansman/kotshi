package se.ansman.kotshi

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.junit.jupiter.api.Test

class SealedClassWithComplexGenericTest {
    private val adapter = Moshi.Builder()
        .add(TestFactory)
        .build()
        .adapter<SealedClassWithComplexGeneric<String, List<Int>>>(
            Types.newParameterizedType(
                SealedClassWithComplexGeneric::class.java,
                String::class.java,
                Types.newParameterizedType(List::class.java, Int::class.javaObjectType)
            )
        )

    @Test
    fun testFromJson() {
        assertThat(adapter.fromJson("""{"type":"type1","a":[1,2,3,4],"b":"hello"}"""))
            .isEqualTo(SealedClassWithComplexGeneric.Type1(listOf(1, 2, 3, 4), "hello"))

        assertThat(adapter.fromJson("""{"type":"type2","error":"err"}"""))
            .isEqualTo(SealedClassWithComplexGeneric.Type2("err"))
    }

    @Test
    fun testToJson() {
        assertThat(adapter.toJson(SealedClassWithComplexGeneric.Type1(listOf(1, 2, 3, 4), "hello")))
            .isEqualTo("""{"type":"type1","a":[1,2,3,4],"b":"hello"}""")

        assertThat(adapter.toJson(SealedClassWithComplexGeneric.Type2("err")))
            .isEqualTo("""{"type":"type2","error":"err"}""")
    }
}