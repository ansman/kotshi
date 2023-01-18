package se.ansman.kotshi

typealias StringList = List<String>

@JsonSerializable
data class Issue201(
    val item: StringList? = null
)