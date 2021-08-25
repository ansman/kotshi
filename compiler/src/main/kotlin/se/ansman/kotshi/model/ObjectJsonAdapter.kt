package se.ansman.kotshi.model

class ObjectJsonAdapter(
    override val targetPackageName: String,
    override val targetSimpleNames: List<String>,
    val polymorphicLabels: Map<String, String>,
) : GeneratableJsonAdapter()