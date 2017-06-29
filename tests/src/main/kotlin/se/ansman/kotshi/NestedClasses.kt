package se.ansman.kotshi

@JsonSerializable
data class NestedClasses(val inner: Inner) {
    @JsonSerializable
    data class Inner(val prop: String)
}