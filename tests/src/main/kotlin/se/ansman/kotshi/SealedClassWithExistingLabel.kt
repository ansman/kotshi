package se.ansman.kotshi

@Polymorphic("type")
@JsonSerializable
sealed class SealedClassWithExistingLabel {
    @PolymorphicLabel("type1")
    @JsonSerializable
    data object Type1 : SealedClassWithExistingLabel()

    @PolymorphicLabel("type2")
    @JsonSerializable
    data class Type2(val name: String, val type: String = "type2") : SealedClassWithExistingLabel()
}