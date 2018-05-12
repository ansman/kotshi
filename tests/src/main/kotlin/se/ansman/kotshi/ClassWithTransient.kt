package se.ansman.kotshi

@JsonSerializable
data class ClassWithTransient(
    @Transient
    val value: String = "",
    val value2: String,
    @Transient
    val list: List<String> = listOf()
)