package se.ansman.kotshi

@JsonSerializable
@Polymorphic(labelKey = "type", onMissing = Polymorphic.Fallback.NULL)
sealed class SealedClassOnMissingNull {
    @JsonSerializable
    @PolymorphicLabel("type1")
    data class Subclass1(val foo: String) : SealedClassOnMissingNull()

    @JsonSerializable
    @JsonDefaultValue
    data object Default : SealedClassOnMissingNull()
}