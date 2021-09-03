package se.ansman.kotshi.model

data class EnumJsonAdapter(
    override val targetPackageName: String,
    override val targetSimpleNames: List<String>,
    val entries: List<Entry>,
    val fallback: Entry?,
) : GeneratableJsonAdapter() {
    init {
        require(fallback == null || fallback in entries)
    }

    data class Entry(
        val name: String,
        val serializedName:String
    )
}