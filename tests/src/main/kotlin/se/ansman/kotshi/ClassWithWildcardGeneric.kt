package se.ansman.kotshi

@JsonSerializable
data class ClassWithWildcardGeneric<out Data>(val data: List<Data>)

@JsonSerializable
data class ClassWithBoundGeneric<out Data : Number>(val data: Data)

@JsonSerializable
data class ParameterizedModel<out Data>(val data: Data)

@JsonSerializable
data class TripleParameterizedModel<out Data, out T : CharSequence, out E : Any>(
    val e: E,
    val data: Data,
    val t: T
)

@JsonSerializable
data class TypeCeption<out D : Any>(
    val tpm: TripleParameterizedModel<ParameterizedModel<IntArray>, String, D>,
    val pm: ParameterizedModel<List<D>>
)