package se.ansman.kotshi

@JsonSerializable
@Polymorphic(labelKey = "type")
sealed class SealedClassWithDefaultWithoutType

@JsonSerializable
@PolymorphicLabel("type1")
data class SealedClassWithDefaultWithoutTypeSubclass1(val foo: String) : SealedClassWithDefaultWithoutType()

@JsonSerializable
@PolymorphicLabel("type2")
data class SealedClassWithDefaultWithoutTypeSubclass2(val bar: String) : SealedClassWithDefaultWithoutType()

@JsonSerializable
@PolymorphicLabel("type3")
data class SealedClassWithDefaultWithoutTypeSubclass3(val baz: String) : SealedClassWithDefaultWithoutType()

@JsonSerializable
@JsonDefaultValue
data object SealedClassWithDefaultWithoutTypeDefault : SealedClassWithDefaultWithoutType()