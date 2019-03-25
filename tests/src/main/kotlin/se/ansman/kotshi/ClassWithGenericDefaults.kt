package se.ansman.kotshi

@JsonSerializable
data class ClassWithGenericDefaults(
    val generic1: Generic1<String?> = Generic1(null, null),
    val generic2: Generic2<String?, Int?> = Generic2(null, null),
    val list: List<List<String>> = emptyList()
) {
    open class Generic2<out T1, out T2>(val t1: T1, val t2: T2)
    class Generic1<out T>(t1: T, t2: String?) : Generic2<T, String?>(t1, t2)
}