package se.ansman.kotshi

@JsonSerializable
@Polymorphic(labelKey = "type")
sealed class SealedClassWithGeneric<out T> {
    @JsonSerializable
    @PolymorphicLabel("success")
    data class Success<T>(val data: T) : SealedClassWithGeneric<T>()

    @JsonSerializable
    @PolymorphicLabel("error")
    data class Error(val error: String) : SealedClassWithGeneric<Nothing>()
}