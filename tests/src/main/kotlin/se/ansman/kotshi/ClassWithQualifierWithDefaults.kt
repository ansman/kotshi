package se.ansman.kotshi

@JsonSerializable
data class ClassWithQualifierWithDefaults(@QualifierWithDefaults val foo: String)