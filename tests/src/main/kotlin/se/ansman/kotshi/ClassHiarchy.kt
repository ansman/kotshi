package se.ansman.kotshi

open class ClassHiarchy(val foo: String) {
    @JsonSerializable
    class Subclass(foo: String) : ClassHiarchy(foo)
}