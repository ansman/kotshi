package se.ansman.kotshi

@JsonSerializable
data class Wildcards(var list: List<CharSequence>) {
    @JsonSerializable
    data class AnyBound(var keys: List<Map<String, Any>>)
}
