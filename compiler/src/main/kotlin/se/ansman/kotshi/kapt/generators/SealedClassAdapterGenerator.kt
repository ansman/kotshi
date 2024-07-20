package se.ansman.kotshi.kapt.generators

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.tag
import kotlin.metadata.KmClass
import kotlin.metadata.Modality
import kotlin.metadata.modality
import se.ansman.kotshi.Errors
import se.ansman.kotshi.Errors.defaultSealedValueIsGeneric
import se.ansman.kotshi.Errors.multipleJsonDefaultValueInSealedClass
import se.ansman.kotshi.JsonDefaultValue
import se.ansman.kotshi.Polymorphic
import se.ansman.kotshi.PolymorphicLabel
import se.ansman.kotshi.kapt.KaptProcessingError
import se.ansman.kotshi.kapt.MetadataAccessor
import se.ansman.kotshi.kapt.logKotshiError
import se.ansman.kotshi.model.GeneratableJsonAdapter
import se.ansman.kotshi.model.GlobalConfig
import se.ansman.kotshi.model.SealedClassJsonAdapter
import se.ansman.kotshi.model.getSealedSubtypes
import javax.annotation.processing.Messager
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

class SealedClassAdapterGenerator(
    metadataAccessor: MetadataAccessor,
    types: Types,
    elements: Elements,
    element: TypeElement,
    metadata: KmClass,
    globalConfig: GlobalConfig,
    messager: Messager,
) : AdapterGenerator(metadataAccessor, types, elements, element, metadata, globalConfig, messager) {
    init {
        require(metadata.modality == Modality.SEALED)
    }

    override fun getGeneratableJsonAdapter(): GeneratableJsonAdapter {
        val implementations = findSealedClassImplementations().toList()
        if (implementations.isEmpty()) {
            throw KaptProcessingError(Errors.noSealedSubclasses, targetElement)
        }

        val annotation = targetElement.getAnnotation(Polymorphic::class.java)!!

        val subtypes = implementations.mapNotNull { subtypeElement ->
            val typeSpec = metadataAccessor.getTypeSpec(subtypeElement)
            SealedClassJsonAdapter.Subtype(
                type = typeSpec.getTypeName(),
                wildcardType = typeSpec.getTypeName { STAR },
                superClass = typeSpec.superclass,
                label = subtypeElement.getAnnotation(PolymorphicLabel::class.java)
                    ?.value
                    ?: return@mapNotNull run {
                        if (KModifier.SEALED !in typeSpec.modifiers && subtypeElement.getAnnotation(JsonDefaultValue::class.java) == null) {
                            messager.logKotshiError(Errors.polymorphicSubclassMustHavePolymorphicLabel, subtypeElement)
                        }
                        null
                    },
            )
        }

        return SealedClassJsonAdapter(
            targetPackageName = targetClassName.packageName,
            targetSimpleNames = targetClassName.simpleNames,
            targetTypeVariables = targetTypeSpec.typeVariables,
            polymorphicLabels = getPolymorphicLabels(),
            labelKey = annotation.labelKey,
            onMissing = annotation.onMissing,
            onInvalid = annotation.onInvalid,
            subtypes = subtypes,
            defaultType = implementations
                .mapNotNull { typeElement ->
                    val typeSpec = metadataAccessor.getTypeSpec(typeElement)
                    if (typeElement.getAnnotation(JsonDefaultValue::class.java) == null) {
                        null
                    } else {
                        if (typeSpec.typeVariables.isNotEmpty()) {
                            throw KaptProcessingError(defaultSealedValueIsGeneric, targetElement)
                        }
                        typeSpec.tag<ClassName>()!!
                    }
                }
                .let { defaultValues ->
                    if (defaultValues.size > 1) {
                        throw KaptProcessingError(multipleJsonDefaultValueInSealedClass, targetElement)
                    } else {
                        defaultValues.singleOrNull()
                    }
                }
        )
    }

    private fun findSealedClassImplementations(): Sequence<TypeElement> =
        (targetElement to kmClass)
            .getSealedSubtypes(
                getSealedSubclasses = {
                    second.sealedSubclasses.asSequence().map {
                        val typeElement = requireNotNull(elements.getTypeElement(it.replace('/', '.'))) {
                            "Could not find element for class $it"
                        }
                        typeElement to metadataAccessor.getKmClass(typeElement)
                    }
                },
                isSealed = { second.modality == Modality.SEALED },
                hasAnnotation = { first.getAnnotation(it) != null },
                getPolymorphicLabelKey = { first.getAnnotation(Polymorphic::class.java)?.labelKey },
                getPolymorphicLabel = { first.getAnnotation(PolymorphicLabel::class.java)?.value },
                error =  { msg, (t) -> KaptProcessingError(msg, t)},
            )
            .map { it.first }
}