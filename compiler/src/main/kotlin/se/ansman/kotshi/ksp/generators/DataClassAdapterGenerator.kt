package se.ansman.kotshi.ksp.generators

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isInternal
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.moshi.Json
import se.ansman.kotshi.Errors
import se.ansman.kotshi.Errors.privateDataClassProperty
import se.ansman.kotshi.ExperimentalKotshiApi
import se.ansman.kotshi.JSON_UNSET_NAME
import se.ansman.kotshi.JsonProperty
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.PrimitiveAdapters
import se.ansman.kotshi.SerializeNulls
import se.ansman.kotshi.ksp.KspProcessingError
import se.ansman.kotshi.ksp.getAnnotation
import se.ansman.kotshi.ksp.getEnumValue
import se.ansman.kotshi.ksp.getValueOrDefault
import se.ansman.kotshi.ksp.isJsonQualifier
import se.ansman.kotshi.ksp.toAnnotationModel
import se.ansman.kotshi.model.DataClassJsonAdapter
import se.ansman.kotshi.model.GeneratableJsonAdapter
import se.ansman.kotshi.model.GlobalConfig
import se.ansman.kotshi.model.Property

class DataClassAdapterGenerator(
    environment: SymbolProcessorEnvironment,
    element: KSClassDeclaration,
    globalConfig: GlobalConfig,
    resolver: Resolver,
) : AdapterGenerator(environment, element, globalConfig, resolver) {

    init {
        require(Modifier.DATA in element.modifiers)
    }

    private val annotation = element.getAnnotation<JsonSerializable>()!!
    private val serializeNulls = annotation
        .getEnumValue("serializeNulls", SerializeNulls.DEFAULT)
        .takeUnless { it == SerializeNulls.DEFAULT }
        ?: globalConfig.serializeNulls

    @OptIn(KspExperimental::class)
    override fun getGeneratableJsonAdapter(): GeneratableJsonAdapter {
        val primaryConstructor = targetElement.primaryConstructor!!
        if (!primaryConstructor.isPublic() && !primaryConstructor.isInternal()) {
            throw KspProcessingError(Errors.privateDataClassConstructor, primaryConstructor)
        }
        return DataClassJsonAdapter(
            targetPackageName = targetClassName.packageName,
            targetSimpleNames = targetClassName.simpleNames,
            targetTypeVariables = targetTypeVariables,
            polymorphicLabels = polymorphicLabels,
            properties = primaryConstructor.parameters.map { it.toProperty() },
            serializeNulls = serializeNulls,
            constructorSignature = resolver.mapToJvmSignature(primaryConstructor)
                ?: throw KspProcessingError("Failed to resolve signature for constructor", primaryConstructor),
        )
    }

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
            throw KspProcessingError(privateDataClassProperty(name), property)
        }

        val jsonAnnotation = getAnnotation<Json>()
        val propertyJsonAnnotation = property.getAnnotation<Json>()

        return Property.create(
            name = name,
            type = type,
            jsonQualifiers = qualifiers,
            globalConfig = globalConfig,
            useAdaptersForPrimitives = annotation.getEnumValue("useAdaptersForPrimitives", PrimitiveAdapters.DEFAULT),
            parameterJsonName = (getAnnotation<JsonProperty>() ?: jsonAnnotation)
                ?.getValueOrDefault<String?>("name") { null }
                ?.takeUnless { it == JSON_UNSET_NAME },
            propertyJsonName = (property.getAnnotation<JsonProperty>() ?: propertyJsonAnnotation)
                ?.getValueOrDefault<String?>("name") { null }
                ?.takeUnless { it == JSON_UNSET_NAME },
            isTransient = property.getAnnotation<Transient>() != null,
            isJsonIgnore = (jsonAnnotation ?: propertyJsonAnnotation)?.getValueOrDefault("ignore") { false },
            hasDefaultValue = hasDefault,
            error = { KspProcessingError(it, property) }
        )
    }
}