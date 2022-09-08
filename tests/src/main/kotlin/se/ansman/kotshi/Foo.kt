package se.ansman.kotshi

@JsonSerializable
@Polymorphic("type")
sealed class Foo {
    @JsonSerializable
    @JsonDefaultValue
    object Bar : Foo()
}