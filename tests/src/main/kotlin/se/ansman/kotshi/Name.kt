package se.ansman.kotshi

@JsonSerializable
data class Name @KotshiConstructor constructor(val firstName: String, val lastName: String) {
    constructor(fullName: String) : this(fullName.substringBefore(" "), fullName.substringAfter(" "))
}
