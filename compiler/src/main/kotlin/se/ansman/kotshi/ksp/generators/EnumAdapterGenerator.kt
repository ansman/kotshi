package se.ansman.kotshi.ksp.generators

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.moshi.Json
import se.ansman.kotshi.Errors
import se.ansman.kotshi.ExperimentalKotshiApi
import se.ansman.kotshi.JsonDefaultValue
import se.ansman.kotshi.JsonProperty
import se.ansman.kotshi.ksp.KspProcessingError
import se.ansman.kotshi.ksp.getAnnotation
import se.ansman.kotshi.ksp.getValue
import se.ansman.kotshi.model.EnumJsonAdapter
import se.ansman.kotshi.model.GeneratableJsonAdapter
import se.ansman.kotshi.model.GlobalConfig

class EnumAdapterGenerator(
    environment: SymbolProcessorEnvironment,
    element: KSClassDeclaration,
    globalConfig: GlobalConfig,
    resolver: Resolver
) : AdapterGenerator(environment, element, globalConfig, resolver) {
    init {
        require(Modifier.ENUM in element.modifiers)
    }

    override fun getGeneratableJsonAdapter(): GeneratableJsonAdapter {
        val entries = targetElement.declarations
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.classKind == ClassKind.ENUM_ENTRY }
            .toList()

        val defaultValues = entries.filter { it.getAnnotation<JsonDefaultValue>() != null }
        if (defaultValues.size > 1) {
            throw KspProcessingError(Errors.multipleJsonDefaultValueInEnum, targetElement)
        }

        return EnumJsonAdapter(
            targetPackageName = targetClassName.packageName,
            targetSimpleNames = targetClassName.simpleNames,
            entries = entries.map { it.toEnumEntry() },
            fallback = defaultValues.singleOrNull()?.toEnumEntry()
        )
    }

    @OptIn(ExperimentalKotshiApi::class)
    private fun KSClassDeclaration.toEnumEntry(): EnumJsonAdapter.Entry =
        EnumJsonAdapter.Entry(
            name = simpleName.getShortName(),
            serializedName = (getAnnotation<JsonProperty>() ?: getAnnotation<Json>())
                ?.getValue("name")
                ?: simpleName.getShortName()
        )
}