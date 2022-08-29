package se.ansman.kotshi.ksp.generators

import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.moshi.Json
import se.ansman.kotshi.ExperimentalKotshiApi
import se.ansman.kotshi.JSON_UNSET_NAME
import se.ansman.kotshi.JsonProperty
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.PrimitiveAdapters
import se.ansman.kotshi.SerializeNulls
import se.ansman.kotshi.ksp.KspProcessingError
import se.ansman.kotshi.ksp.getAnnotation
import se.ansman.kotshi.ksp.getEnumValue
import se.ansman.kotshi.ksp.getValue
import se.ansman.kotshi.ksp.isJsonQualifier
import se.ansman.kotshi.ksp.toAnnotationModel
import se.ansman.kotshi.model.DataClassJsonAdapter
import se.ansman.kotshi.model.GeneratableJsonAdapter
import se.ansman.kotshi.model.GlobalConfig
import se.ansman.kotshi.model.Property

class DataClassAdapterGenerator(
    environment: SymbolProcessorEnvironment,
    element: KSClassDeclaration,
    globalConfig: GlobalConfig
) : AdapterGenerator(environment, element, globalConfig) {

    init {
        require(Modifier.DATA in element.modifiers)
    }

    private val annotation = element.getAnnotation<JsonSerializable>()!!
    private val serializeNulls = annotation
        .getEnumValue("serializeNulls", SerializeNulls.DEFAULT)
        .takeUnless { it == SerializeNulls.DEFAULT }
        ?: globalConfig.serializeNulls

    override fun getGenerableAdapter(): GeneratableJsonAdapter =
        DataClassJsonAdapter(
            targetPackageName = targetClassName.packageName,
            targetSimpleNames = targetClassName.simpleNames,
            targetTypeVariables = targetTypeVariables,
            polymorphicLabels = polymorphicLabels,
            properties = targetElement.primaryConstructor!!.parameters.map { it.toProperty() },
            serializeNulls = serializeNulls,
        )

    @OptIn(ExperimentalKotshiApi::class)
    private fun KSValueParameter.toProperty(): Property {
        val name = name!!.asString()
        val type = type.toTypeName(typeParameterResolver)

        val property = targetElement.getDeclaredProperties().find { it.simpleName == this.name }
            ?: throw KspProcessingError("Could not find property for parameter $name", this)

        val qualifiers = annotations
            .filter { it.isJsonQualifier() }
            .map { it.toAnnotationModel() }
            .toList()

        if (Modifier.PRIVATE in property.modifiers || Modifier.PROTECTED in property.modifiers) {
            throw KspProcessingError("Property $name must be public or internal", property)
        }


        val jsonAnnotation = getAnnotation<Json>()
        val propertyJsonAnnotation = property.getAnnotation<Json>()
        @Suppress("RemoveExplicitTypeArguments")
        val isTransient = property.getAnnotation<Transient>() != null ||
            (jsonAnnotation ?: propertyJsonAnnotation)?.getValue<Boolean?>("ignore") ?: false

        if (isTransient && !hasDefault) {
            throw KspProcessingError("Transient property $name must declare a default value", property)
        }
        return Property.create(
            name = name,
            type = type,
            jsonQualifiers = qualifiers,
            globalConfig = globalConfig,
            useAdaptersForPrimitives = annotation.getEnumValue("useAdaptersForPrimitives", PrimitiveAdapters.DEFAULT),
            parameterJsonName = (getAnnotation<JsonProperty>() ?: jsonAnnotation)
                ?.getValue<String?>("name")
                ?.takeUnless { it == JSON_UNSET_NAME },
            propertyJsonName = (property.getAnnotation<JsonProperty>() ?: propertyJsonAnnotation)
                ?.getValue<String?>("name")
                ?.takeUnless { it == JSON_UNSET_NAME },
            isTransient = isTransient,
            hasDefaultValue = hasDefault
        )
    }
}