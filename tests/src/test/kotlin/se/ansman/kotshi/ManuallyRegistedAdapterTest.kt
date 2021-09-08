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

class ManuallyRegistedAdapterTest {
    private val moshi = Moshi.Builder()
        .add(TestFactory)
        .build()

    @Test
    fun testRegistersRegularAdapter() {
        assertThat(moshi.adapter(ManuallyRegistedAdapter.Type::class.java))
            .isSameAs(ManuallyRegistedAdapter)
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
        val adapter = moshi.adapter<String>(typeOf<ManuallyRegistedGenericAdapter.GenericType<Int>>().javaType)
        assertIs<ManuallyRegistedGenericAdapter<*>>(adapter)
        assertSame(moshi, adapter.moshi)
        assertThat(adapter.types)
            .asList()
            .containsExactly(Int::class.javaObjectType)
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testManuallyRegistedWrappedGenericAdapter() {
        assertFailsWith<IllegalArgumentException> {
            moshi.adapter<ManuallyRegistedWrappedGenericAdapter.GenericType<List<Int>>>()
        }
        assertIs<ManuallyRegistedWrappedGenericAdapter>(
            moshi.adapter<ManuallyRegistedWrappedGenericAdapter.GenericType<List<String>>>(
                typeOf<ManuallyRegistedWrappedGenericAdapter.GenericType<List<String>>>().javaType
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