package se.ansman.kotshi

@JsonSerializable(serializeNulls = SerializeNulls.ENABLED)
data class ClassWithSerializeNulls(val nested: Nested?) {
    @JsonSerializable
    data class Nested(val value: String?)
}