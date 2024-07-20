package se.ansman.kotshi.kapt.generators

import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.tag
import kotlin.metadata.KmClass
import kotlin.metadata.KmConstructor
import kotlin.metadata.isData
import kotlin.metadata.jvm.signature
import se.ansman.kotshi.Errors
import se.ansman.kotshi.Errors.privateDataClassProperty
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.kapt.KaptProcessingError
import se.ansman.kotshi.kapt.MetadataAccessor
import se.ansman.kotshi.kapt.isJsonIgnore
import se.ansman.kotshi.kapt.jsonName
import se.ansman.kotshi.kapt.qualifiers
import se.ansman.kotshi.model.DataClassJsonAdapter
import se.ansman.kotshi.model.GeneratableJsonAdapter
import se.ansman.kotshi.model.GlobalConfig
import se.ansman.kotshi.model.Property
import javax.annotation.processing.Messager
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

class DataClassAdapterGenerator(
    metadataAccessor: MetadataAccessor,
    types: Types,
    elements: Elements,
    element: TypeElement,
    metadata: KmClass,
    globalConfig: GlobalConfig,
    messager: Messager,
) : AdapterGenerator(metadataAccessor, types, elements, element, metadata, globalConfig, messager) {
    init {
        require(metadata.isData)
    }

    private val config = element.getAnnotation(JsonSerializable::class.java)

    override fun getGeneratableJsonAdapter(): GeneratableJsonAdapter {
        val primaryConstructor = targetTypeSpec.primaryConstructor!!
        if (KModifier.PRIVATE in primaryConstructor.modifiers || KModifier.PROTECTED in primaryConstructor.modifiers) {
            throw KaptProcessingError(Errors.privateDataClassConstructor, targetElement)
        }
        return DataClassJsonAdapter(
            targetPackageName = targetClassName.packageName,
            targetSimpleNames = targetClassName.simpleNames,
            targetTypeVariables = targetTypeSpec.typeVariables,
            polymorphicLabels = getPolymorphicLabels(),
            properties = primaryConstructor
                .parameters
                .map { parameter -> parameter.toProperty() },
            serializeNulls = config.serializeNulls,
            constructorSignature = primaryConstructor.tag<KmConstructor>()!!.signature!!.toString()
        )
    }

    private fun ParameterSpec.toProperty(): Property {
        val property = targetTypeSpec.propertySpecs.find { it.name == name }
            ?: throw KaptProcessingError("Internal error! Could not find property for parameter $name. Please file an issue at https://github.com/ansman/kotshi", targetElement)


        if (KModifier.PRIVATE in property.modifiers || KModifier.PROTECTED in property.modifiers) {
            throw KaptProcessingError(privateDataClassProperty(name), targetElement)
        }

        return Property.create(
            name = name,
            type = type.copy(annotations = emptyList()),
            jsonQualifiers = annotations.qualifiers(metadataAccessor),
            globalConfig = globalConfig,
            useAdaptersForPrimitives = config.useAdaptersForPrimitives,
            parameterJsonName = annotations.jsonName(),
            propertyJsonName = property.annotations.jsonName(),
            isTransient = property.annotations.any { it.typeName == se.ansman.kotshi.Types.Kotlin.transient },
            isJsonIgnore = annotations.isJsonIgnore() ?: property.annotations.isJsonIgnore(),
            hasDefaultValue = defaultValue != null,
            error = { KaptProcessingError(it, targetElement) }
        )
    }
}