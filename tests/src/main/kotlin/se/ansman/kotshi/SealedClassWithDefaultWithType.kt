package se.ansman.kotshi

@JsonSerializable
@Polymorphic(labelKey = "type")
sealed class SealedClassWithDefaultWithType {
    @JsonSerializable
    @PolymorphicLabel("type1")
    data class Subclass1(val foo: String) : SealedClassWithDefaultWithType()

    @JsonSerializable
    @PolymorphicLabel("type2")
    data class Subclass2(val bar: String) : SealedClassWithDefaultWithType()

    @JsonSerializable
    @PolymorphicLabel("type3")
    data class Subclass3(val baz: String) : SealedClassWithDefaultWithType()

    @JsonSerializable
    @JsonDefaultValue
    @PolymorphicLabel("type4")
    data object Default : SealedClassWithDefaultWithType()
}