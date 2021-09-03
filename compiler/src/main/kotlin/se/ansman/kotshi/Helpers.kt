package se.ansman.kotshi

fun <T> T.getPolymorphicLabels(
    supertypes: T.() -> Sequence<T>,
    hasAnnotation: T.(annotationType: Class<out Annotation>) -> Boolean,
    getPolymorphicLabelKey: T.() -> String?,
    getPolymorphicLabel: T.() -> String?,
    error: (String, T) -> Throwable,
): Map<String, String> {

    val output = LinkedHashMap<String, String>()

    for (type in supertypes() + sequenceOf(this)) {
        val labelKey = type.supertypes()
            .mapNotNull { it.getPolymorphicLabelKey() }
            .firstOrNull()
            ?: continue
        output[labelKey] = type.getPolymorphicLabel() ?: continue
    }
    if (output.isEmpty() && hasAnnotation(PolymorphicLabel::class.java)) {
        throw error("Found no @PolymorphicLabel annotations", this)
    }
    return output
}