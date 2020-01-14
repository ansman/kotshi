package se.ansman.kotshi

@JsonSerializable
@Polymorphic("type")
sealed class NestedSealedClass {

    @JsonSerializable
    @Polymorphic("type")
    sealed class Nested1 : NestedSealedClass() {
        @JsonSerializable
        @PolymorphicLabel("nestedChild1")
        data class NestedChild1(val v: String) : Nested1()

        @JsonSerializable
        @Polymorphic("type")
        sealed class Nested2 : Nested1() {
            @JsonSerializable
            @PolymorphicLabel("nestedChild2")
            data class NestedChild2(val v: String) : Nested2()
        }
    }

    @JsonSerializable
    @PolymorphicLabel("nestedChild3")
    data class NestedChild3(val v: String) : NestedSealedClass()

    @JsonSerializable
    @PolymorphicLabel("nested3")
    @Polymorphic("subtype")
    sealed class Nested3 : NestedSealedClass() {
        @JsonSerializable
        @PolymorphicLabel("nestedChild4")
        data class NestedChild4(val v: String) : Nested3()

        @JsonSerializable
        @PolymorphicLabel("nestedChild5")
        data class NestedChild5(val v: String) : Nested3()

    }
}