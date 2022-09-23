package se.ansman.kotshi.model

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeVariableName
import se.ansman.kotshi.*
import se.ansman.kotshi.Errors.nestedSealedClassHasPolymorphicLabel
import se.ansman.kotshi.Errors.nestedSealedClassMissingPolymorphicLabel
import se.ansman.kotshi.Errors.nestedSealedClassMustBePolymorphic

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
    labelKey: String = getPolymorphicLabelKey() ?: throw error(Errors.sealedClassMustBePolymorphic, this)
): Sequence<T> = getSealedSubclasses().flatMap {
    if (!it.hasAnnotation(JsonSerializable::class.java)) {
        throw error(Errors.polymorphicSubclassMustHaveJsonSerializable, it)
    }
    when {
        it.isSealed() -> {
            val subclassLabelKey = it.getPolymorphicLabelKey()
                ?: throw error(nestedSealedClassMustBePolymorphic, it)

            val subclassLabel = it.getPolymorphicLabel()

            when {
                subclassLabelKey == labelKey -> {
                    if (subclassLabel != null) {
                        throw error(nestedSealedClassHasPolymorphicLabel, it)
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
                    throw error(nestedSealedClassMissingPolymorphicLabel, it)
                }
                else -> sequenceOf(it)
            }
        }
        !it.hasAnnotation(PolymorphicLabel::class.java) && !it.hasAnnotation(JsonDefaultValue::class.java) ->
            throw error(Errors.polymorphicSubclassMustHavePolymorphicLabel, it)
        else -> sequenceOf(it)
    }
}