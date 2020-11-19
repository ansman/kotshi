package se.ansman.kotshi

typealias AliasedString = String
typealias AliasedList = List<String>
typealias AliasedMap = Map<String, AliasedList>

@JsonSerializable
data class ClassWithTypealiases(
    val string: AliasedString,
    val list: AliasedList,
    val map: AliasedMap
)