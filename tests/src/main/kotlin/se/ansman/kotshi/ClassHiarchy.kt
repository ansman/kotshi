package se.ansman.kotshi

open class ClassHiarchy(open val foo: String) {

    @JsonSerializable
    data class Subclass(override val foo: String) : ClassHiarchy(foo)
}