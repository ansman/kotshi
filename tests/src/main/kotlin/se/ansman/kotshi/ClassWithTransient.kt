package se.ansman.kotshi

@JsonSerializable
data class ClassWithTransient(
    val value: String,
    @Transient
    val value2: String,
    @Transient
    val list: List<String> = listOf()
) {
    companion object {
        @JsonDefaultValue
        fun provideListDefault() = listOf<String>()
    }
}
