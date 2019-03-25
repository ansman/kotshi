package se.ansman.kotshi

open class ClassHierarchy(open val foo: String) {

    @JsonSerializable
    data class Subclass(override val foo: String) : ClassHierarchy(foo)
}