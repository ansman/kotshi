package se.ansman.kotshi

@JsonSerializable
@Polymorphic(labelKey = "type", onInvalid = Polymorphic.Fallback.NULL)
sealed class SealedClassOnInvalidNull {
    @JsonSerializable
    @PolymorphicLabel("type1")
    data class Subclass1(val foo: String) : SealedClassOnInvalidNull()

    @JsonSerializable
    @JsonDefaultValue
    data object Default : SealedClassOnInvalidNull()
}