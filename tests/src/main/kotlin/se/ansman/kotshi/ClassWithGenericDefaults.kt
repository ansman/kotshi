package se.ansman.kotshi

@JsonSerializable
data class ClassWithGenericDefaults(
    @ClassWithGenericDefaultsDefaultValue
    val generic1: Generic1<String?>,
    @ClassWithGenericDefaultsDefaultValue
    val generic2: Generic2<String?, Int?>,
    @ClassWithGenericDefaultsDefaultValue
    val list: List<List<String>>
) {

    open class Generic2<out T1, out T2>(val t1: T1, val t2: T2)
    class Generic1<out T>(t1: T, t2: String?) : Generic2<T, String?>(t1, t2)

    companion object {
        @ClassWithGenericDefaultsDefaultValue
        fun <T> provideGeneric1Default(): Generic1<T?> = Generic1(null, null)

        @ClassWithGenericDefaultsDefaultValue
        fun <T1, T2> provideGeneric2Default(): Generic2<T1?, T2?> = Generic2(null, null)

        @ClassWithGenericDefaultsDefaultValue
        fun <T> emptyList(): List<T> = emptyList()
    }
}

@JsonDefaultValue
annotation class ClassWithGenericDefaultsDefaultValue