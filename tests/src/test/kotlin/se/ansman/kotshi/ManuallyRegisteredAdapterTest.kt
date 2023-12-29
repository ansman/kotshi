package se.ansman.kotshi

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotInstanceOf
import assertk.assertions.isSameInstanceAs
import com.squareup.moshi.Moshi
import org.junit.jupiter.api.Test
import kotlin.reflect.javaType
import kotlin.reflect.typeOf

class ManuallyRegisteredAdapterTest {
    private val moshi = Moshi.Builder()
        .add(TestFactory)
        .build()

    @Test
    fun testRegistersRegularAdapter() {
        assertThat(moshi.adapter(ManuallyRegisteredAdapter.Type::class.java))
            .isSameInstanceAs(ManuallyRegisteredAdapter)
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
        assertThat(adapter)
            .isInstanceOf<ManuallyRegisteredGenericAdapter<*>>()
            .given {
                assertThat(it.moshi).isSameInstanceAs(moshi)
                assertThat(it.types).containsExactly(Int::class.javaObjectType)
            }
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testManuallyRegisteredWrappedGenericAdapter() {
        assertThat(
            moshi.adapter<ManuallyRegisteredWrappedGenericAdapter.GenericType<List<String>>>(
                typeOf<ManuallyRegisteredWrappedGenericAdapter.GenericType<List<String>>>().javaType
            )
        ).isInstanceOf<ManuallyRegisteredWrappedGenericAdapter>()
        if (!usingLegacyMoshi) {
            // Legacy moshi doesn't throw on unsupported Kotlin types
            assertFailure { moshi.adapter<ManuallyRegisteredWrappedGenericAdapter.GenericType<List<Int>>>() }
                .isInstanceOf<IllegalArgumentException>()
        }
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