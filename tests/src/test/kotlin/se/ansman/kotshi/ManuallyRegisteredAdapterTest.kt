package se.ansman.kotshi

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import org.junit.Test
import kotlin.reflect.javaType
import kotlin.reflect.typeOf
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame

class ManuallyRegisteredAdapterTest {
    private val moshi = Moshi.Builder()
        .add(TestFactory)
        .build()

    @Test
    fun testRegistersRegularAdapter() {
        assertThat(moshi.adapter(ManuallyRegisteredAdapter.Type::class.java))
            .isSameAs(ManuallyRegisteredAdapter)
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testCanOverwrite() {
        assertThat(moshi.adapter<OverwrittenAdapter.Data<String>>(typeOf<OverwrittenAdapter.Data<String>>().javaType))
            .isInstanceOf(OverwrittenAdapter::class.java)
        assertThat(moshi.adapter<OverwrittenAdapter.Data<Int>>(typeOf<OverwrittenAdapter.Data<Int>>().javaType))
            .isNotInstanceOf(OverwrittenAdapter::class.java)
    }

    @Test
    fun testWithQualifier() {
        assertThat(moshi.adapter<String>(
            String::class.java,
            setOf(QualifierWithDefaults(string = "Goodbye"), Hello())
        ))
            .isInstanceOf(QualifiedAdapter::class.java)
        assertThat(moshi.adapter(String::class.java))
            .isNotInstanceOf(OverwrittenAdapter::class.java)
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testAbstractAdapters() {
        assertThat(moshi.adapter<Nothing>(typeOf<List<AbstractAdapter.RealAdapter.Data<String>>>().javaType))
            .isInstanceOf(AbstractAdapter.RealAdapter::class.java)

        assertThat(moshi.adapter<Nothing>(typeOf<List<AbstractAdapter.RealAdapter.Data<Int>>>().javaType))
            .isNotInstanceOf(AbstractAdapter.RealAdapter::class.java)

        assertThat(moshi.adapter<Nothing>(typeOf<List<AbstractAdapter.GenericAdapter.Data<String>>>().javaType))
            .isInstanceOf(AbstractAdapter.GenericAdapter::class.java)
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testRegistersGenericAdapter() {
        val adapter = moshi.adapter<String>(typeOf<ManuallyRegisteredGenericAdapter.GenericType<Int>>().javaType)
        assertIs<ManuallyRegisteredGenericAdapter<*>>(adapter)
        assertSame(moshi, adapter.moshi)
        assertThat(adapter.types)
            .asList()
            .containsExactly(Int::class.javaObjectType)
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testManuallyRegisteredWrappedGenericAdapter() {
        assertFailsWith<IllegalArgumentException> {
            moshi.adapter<ManuallyRegisteredWrappedGenericAdapter.GenericType<List<Int>>>()
        }
        assertIs<ManuallyRegisteredWrappedGenericAdapter>(
            moshi.adapter<ManuallyRegisteredWrappedGenericAdapter.GenericType<List<String>>>(
                typeOf<ManuallyRegisteredWrappedGenericAdapter.GenericType<List<String>>>().javaType
            )
        )
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testPrioritizedAdapters() {
        assertThat(moshi.adapter<Nothing>(typeOf<PrioritizedAdapter.Data<String>>().javaType))
            .isInstanceOf(PrioritizedAdapter.CStringAdapter::class.java)

        assertThat(moshi.adapter<Nothing>(typeOf<PrioritizedAdapter.Data<Boolean>>().javaType))
            .isInstanceOf(PrioritizedAdapter.BGenericAdapter::class.java)

        assertThat(moshi.adapter<Nothing>(typeOf<PrioritizedAdapter.Data<Int>>().javaType))
            .isInstanceOf(PrioritizedAdapter.BGenericAdapter::class.java)
    }
}