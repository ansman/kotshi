package se.ansman.kotshi.model

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeVariableName
import se.ansman.kotshi.JsonDefaultValue
import se.ansman.kotshi.Polymorphic
import se.ansman.kotshi.PolymorphicLabel

data class SealedClassJsonAdapter(
    override val targetPackageName: String,
    override val targetSimpleNames: List<String>,
    override val targetTypeVariables: List<TypeVariableName>,
    val polymorphicLabels: Map<String, String>,
    val labelKey: String,
    val onMissing: Polymorphic.Fallback,
    val onInvalid: Polymorphic.Fallback,
    val subtypes: List<Subtype>,
    val defaultType: ClassName?,
) : GeneratableJsonAdapter() {
    init {
        if (defaultType != null) {
            require(onMissing == Polymorphic.Fallback.DEFAULT || onInvalid == Polymorphic.Fallback.DEFAULT) {
                "Using @JsonDefaultValue in combination with onMissing=$onMissing and onInvalid=$onInvalid makes no sense"
            }
        }
    }

    data class Subtype(
        val type: TypeName,
        val wildcardType: TypeName,
        val superClass: TypeName,
        val label: String
    )
}

fun <T> T.getSealedSubtypes(
    getSealedSubclasses: T.() -> Sequence<T>,
    isSealed: T.() -> Boolean,
    hasAnnotation: T.(Class<out Annotation>) -> Boolean,
    getPolymorphicLabelKey: T.() -> String?,
    getPolymorphicLabel: T.() -> String?,
    error: (String, T) -> Throwable,
    labelKey: String = getPolymorphicLabelKey(this) ?: throw error(
        "Sealed classes must be annoted with @Polymorphic",
        this
    )
): Sequence<T> = getSealedSubclasses().flatMap {
    when {
        it.isSealed() -> {
            val subclassLabelKey = it.getPolymorphicLabelKey()
                ?: throw error("Children of a sealed class must be annotated with @Polymorphic", it)

            val subclassLabel = it.getPolymorphicLabel()

            when {
                subclassLabelKey == labelKey -> {
                    if (subclassLabel != null) {
                        throw error(
                            "Children of a sealed class with the same label key must not be annotated with @PolymorphicLabel",
                            it
                        )
                    }
                    it.getSealedSubtypes(
                        getSealedSubclasses = getSealedSubclasses,
                        isSealed = isSealed,
                        hasAnnotation = hasAnnotation,
                        getPolymorphicLabelKey = getPolymorphicLabelKey,
                        getPolymorphicLabel = getPolymorphicLabel,
                        error = error,
                        labelKey = subclassLabelKey,
                    )
                }
                subclassLabel == null -> {
                    throw error(
                        "Children of a sealed class with a different label key must be annotated with @PolymorphicLabel",
                        it
                    )
                }
                else -> sequenceOf(it)
            }
        }
        !it.hasAnnotation(PolymorphicLabel::class.java) && !it.hasAnnotation(JsonDefaultValue::class.java) ->
            throw error(
                "Subclasses of sealed classes must be annotated with @PolymorphicLabel or @JsonDefaultValue",
                it
            )
        else -> sequenceOf(it)
    }
}