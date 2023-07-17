package se.ansman.kotshi

@JsonSerializable
@Polymorphic(labelKey = "type", onInvalid = Polymorphic.Fallback.FAIL)
sealed class SealedClassOnInvalidFail {
    @JsonSerializable
    @PolymorphicLabel("type1")
    data class Subclass1(val foo: String) : SealedClassOnInvalidFail()

    @JsonSerializable
    @JsonDefaultValue
    data object Default : SealedClassOnInvalidFail()
}