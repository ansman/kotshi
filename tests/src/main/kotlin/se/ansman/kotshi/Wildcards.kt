package se.ansman.kotshi

@JsonSerializable
data class Wildcards(val list: List<CharSequence>) {
    @JsonSerializable
    data class AnyBound(val keys: List<Map<String, Any>>)
}
