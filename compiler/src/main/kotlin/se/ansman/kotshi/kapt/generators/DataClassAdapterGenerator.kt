package se.ansman.kotshi.kapt.generators

import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.metadata.ImmutableKmClass
import com.squareup.kotlinpoet.metadata.isData
import com.squareup.kotlinpoet.tag
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.kapt.KaptProcessingError
import se.ansman.kotshi.kapt.MetadataAccessor
import se.ansman.kotshi.kapt.jsonName
import se.ansman.kotshi.kapt.qualifiers
import se.ansman.kotshi.model.DataClassJsonAdapter
import se.ansman.kotshi.model.GeneratableJsonAdapter
import se.ansman.kotshi.model.GlobalConfig
import se.ansman.kotshi.model.Property
import se.ansman.kotshi.unwrapTypeAlias
import javax.annotation.processing.Messager
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

class DataClassAdapterGenerator(
    metadataAccessor: MetadataAccessor,
    types: Types,
    elements: Elements,
    element: TypeElement,
    metadata: ImmutableKmClass,
    globalConfig: GlobalConfig,
    messager: Messager,
) : AdapterGenerator(metadataAccessor, types, elements, element, metadata, globalConfig, messager) {
    init {
        require(metadata.isData)
    }

    private val config = element.getAnnotation(JsonSerializable::class.java)

    override fun getGenerableAdapter(): GeneratableJsonAdapter =
        DataClassJsonAdapter(
            targetPackageName = targetClassName.packageName,
            targetSimpleNames = targetClassName.simpleNames,
            targetTypeVariables = targetTypeSpec.typeVariables,
            polymorphicLabels = getPolymorphicLabels(),
            properties = targetTypeSpec.primaryConstructor
                ?.parameters
                ?.map { parameter -> parameter.toProperty() }
                ?.takeUnless { it.isEmpty() }
                ?: throw KaptProcessingError("Could not find any data class properties.", targetElement),
            serializeNulls = config.serializeNulls
        )

    private fun ParameterSpec.toProperty(): Property {
        val property = targetTypeSpec.propertySpecs.find { it.name == name }
            ?: throw KaptProcessingError("Could not find property for parameter $name", targetElement)


        if (KModifier.PRIVATE in property.modifiers || KModifier.PROTECTED in property.modifiers) {
            throw KaptProcessingError("Property $name must be public or internal", targetElement)
        }

        val isTransient = property.annotations.any { it.typeName == se.ansman.kotshi.Types.Kotlin.transient }
        if (isTransient && defaultValue == null) {
            throw KaptProcessingError("Transient property $name must declare a default value", tag()!!)
        }

        return Property.create(
            name = name,
            type = type.unwrapTypeAlias().copy(annotations = emptyList()),
            jsonQualifiers = annotations.qualifiers(),
            globalConfig = globalConfig,
            useAdaptersForPrimitives = config.useAdaptersForPrimitives,
            parameterJsonName = annotations.jsonName(),
            propertyJsonName = property.annotations.jsonName(),
            isTransient = isTransient,
            hasDefaultValue = defaultValue != null,
        )
    }
}