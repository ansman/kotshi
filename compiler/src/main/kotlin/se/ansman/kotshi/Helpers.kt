package se.ansman.kotshi

fun <T> T.getPolymorphicLabels(
    supertypes: T.() -> Sequence<T>,
    getPolymorphicLabelKey: T.() -> String?,
    getPolymorphicLabel: T.() -> String?,
): Map<String, String> {
    val output = LinkedHashMap<String, String>()
    for (type in supertypes() + sequenceOf(this)) {
        val labelKey = type.supertypes()
            .mapNotNull { it.getPolymorphicLabelKey() }
            .firstOrNull()
            ?: continue
        output[labelKey] = type.getPolymorphicLabel() ?: continue
    }
    return output
}