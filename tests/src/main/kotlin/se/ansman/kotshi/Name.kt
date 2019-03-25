package se.ansman.kotshi

@JsonSerializable
data class Name(val firstName: String, val lastName: String) {
    constructor(fullName: String) : this(fullName.substringBefore(" "), fullName.substringAfter(" "))

    val fullName: String = "$firstName $lastName"
}
