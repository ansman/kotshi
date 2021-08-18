package se.ansman.kotshi.ksp.generators

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.jvm.throws
import com.squareup.moshi.Json
import se.ansman.kotshi.GlobalConfig
import se.ansman.kotshi.JsonDefaultValue
import se.ansman.kotshi.addControlFlow
import se.ansman.kotshi.addNextControlFlow
import se.ansman.kotshi.addWhen
import se.ansman.kotshi.kapt.generators.ioException
import se.ansman.kotshi.kapt.generators.jsonDataException
import se.ansman.kotshi.kapt.generators.jsonReaderToken
import se.ansman.kotshi.kapt.generators.readerParameter
import se.ansman.kotshi.kapt.generators.writerParameter
import se.ansman.kotshi.ksp.KspProcessingError
import se.ansman.kotshi.ksp.getAnnotation
import se.ansman.kotshi.ksp.getValue
import se.ansman.kotshi.nullable

class EnumAdapterGenerator(
    environment: SymbolProcessorEnvironment,
    resolver: Resolver,
    element: KSClassDeclaration,
    globalConfig: GlobalConfig
) : AdapterGenerator(environment, resolver, element, globalConfig) {
    init {
        require(Modifier.ENUM in element.modifiers)
    }

    override fun TypeSpec.Builder.addMethods() {
        val enumConstants = element.declarations
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.classKind == ClassKind.ENUM_ENTRY }
            .toList()

        val enumToJsonName = enumConstants.associateBy({ it }) { constant ->
            constant.getAnnotation<Json>()?.getValue("name") ?: constant.simpleName.getShortName()
        }

        var defaultValue: String? = null
        for ((entry, name) in enumToJsonName) {
            val jsonDefaultValue = entry.getAnnotation<JsonDefaultValue>()
            if (jsonDefaultValue != null) {
                if (defaultValue != null) {
                    throw KspProcessingError("Only one enum entry can be annotated with @JsonDefaultValue", element)
                }
                defaultValue = name
            }
        }

        this
            .addFunction(FunSpec.builder("toJson")
                .addModifiers(KModifier.OVERRIDE)
                .throws(ioException)
                .addParameter(writerParameter)
                .addParameter(value)
                .addWhen("%N", value) {
                    for ((entry, name) in enumToJsonName) {
                        addStatement("%T.%N·-> %N.value(%S)", className, entry.simpleName.getShortName(), writerParameter, name)
                    }
                    addStatement("null·-> %N.nullValue()", writerParameter)
                }
                .build())
            .addFunction(FunSpec.builder("fromJson")
                .addModifiers(KModifier.OVERRIDE)
                .throws(ioException)
                .addParameter(readerParameter)
                .returns(typeName.nullable())
                .addControlFlow(
                    "return·if (%N.peek() == %T.NULL)",
                    readerParameter,
                    jsonReaderToken,
                    close = false
                ) {
                    addStatement("%N.nextNull()", readerParameter)
                }
                .addNextControlFlow("else when (%N.selectString(options))", readerParameter) {
                    enumToJsonName.keys.forEachIndexed { index, entry ->
                        addStatement("$index·-> %T.%N", className, entry.simpleName.getShortName())
                    }
                    if (defaultValue == null) {
                        addStatement(
                            "else·-> throw·%T(%P)",
                            jsonDataException,
                            "Expected one of ${enumToJsonName.values} but was \${${readerParameter.name}.nextString()} at path \${${readerParameter.name}.path}"
                        )
                    } else {
                        addControlFlow("else·->") {
                            addStatement("%N.skipValue()", readerParameter)
                            addStatement("%T.%N", className, defaultValue)
                        }
                    }
                }
                .build())
            .maybeAddCompanion(enumToJsonName.values)
    }
}