package se.ansman.kotshi

@JsonSerializable
data class ClassWithQualifierWithDefaults(
    @QualifierWithDefaults
    val prop1: String,
    @QualifierWithDefaults("not", "default")
    val prop2: Int,
    @QualifierWithDefaults(string = "not default")
    val prop3: Boolean
)