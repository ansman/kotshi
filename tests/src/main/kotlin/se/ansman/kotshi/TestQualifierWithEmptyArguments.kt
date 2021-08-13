package se.ansman.kotshi

@JsonSerializable
data class TestQualifierWithEmptyArguments(
    @QualifierWithArgumentsArray
    val foo: String
)