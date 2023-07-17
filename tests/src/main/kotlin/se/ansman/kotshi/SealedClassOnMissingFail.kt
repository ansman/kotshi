package se.ansman.kotshi

@JsonSerializable
@Polymorphic(labelKey = "type", onMissing = Polymorphic.Fallback.FAIL)
sealed class SealedClassOnMissingFail {
    @JsonSerializable
    @PolymorphicLabel("type1")
    data class Subclass1(val foo: String) : SealedClassOnMissingFail()

    @JsonSerializable
    @JsonDefaultValue
    data object Default : SealedClassOnMissingFail()
}