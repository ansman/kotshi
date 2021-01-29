package se.ansman.kotshi

@JsonSerializable
@Polymorphic(labelKey = "type")
sealed class SealedClassWithComplexGeneric<out A, B> {
    @JsonSerializable
    @PolymorphicLabel("type1")
    data class Type1<A, B>(val a: List<A>, val b: B) : SealedClassWithComplexGeneric<B, List<A>>()

    @JsonSerializable
    @PolymorphicLabel("type2")
    data class Type2<T>(val error: String) : SealedClassWithComplexGeneric<Nothing, T>()
}