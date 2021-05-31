package se.ansman.kotshi.kapt.generators

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.jvm.throws
import com.squareup.kotlinpoet.metadata.ImmutableKmClass
import com.squareup.kotlinpoet.metadata.isEnum
import se.ansman.kotshi.GlobalConfig
import se.ansman.kotshi.kapt.MetadataAccessor
import se.ansman.kotshi.kapt.ProcessingError
import se.ansman.kotshi.addControlFlow
import se.ansman.kotshi.addNextControlFlow
import se.ansman.kotshi.addWhen
import se.ansman.kotshi.jsonName
import se.ansman.kotshi.nullable
import javax.annotation.processing.Messager
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

class EnumAdapterGenerator(
    metadataAccessor: MetadataAccessor,
    types: Types,
    elements: Elements,
    element: TypeElement,
    metadata: ImmutableKmClass,
    globalConfig: GlobalConfig,
    messager: Messager
) : AdapterGenerator(metadataAccessor, types, elements, element, metadata, globalConfig) {
    init {
        require(metadata.isEnum)
    }

    override fun TypeSpec.Builder.addMethods() {
        val enumToJsonName = elementTypeSpec.enumConstants.mapValues { (name, type) ->
            type.annotationSpecs.jsonName() ?: name
        }

        var defaultValue: String? = null
        for ((entry, spec) in elementTypeSpec.enumConstants) {
            if (spec.annotationSpecs.any { it.className == jsonDefaultValue }) {
                if (defaultValue != null) {
                    throw ProcessingError("Only one enum entry can be annotated with @JsonDefaultValue", element)
                }
                defaultValue = entry
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
                        addStatement("%T.%N·-> %N.value(%S)", className, entry, writerParameter, name)
                    }
                    addStatement("null·-> %N.nullValue()", writerParameter)
                }
                .build())
            .addFunction(FunSpec.builder("fromJson")
                .addModifiers(KModifier.OVERRIDE)
                .throws(ioException)
                .addParameter(readerParameter)
                .returns(typeName.nullable())
                .addControlFlow("return·if (%N.peek() == %T.NULL)", readerParameter, jsonReaderToken, close = false) {
                    addStatement("%N.nextNull()", readerParameter)
                }
                .addNextControlFlow("else when (%N.selectString(options))", readerParameter) {
                    enumToJsonName.keys.forEachIndexed { index, entry ->
                        addStatement("$index·-> %T.%N", className, entry)
                    }
                    if (defaultValue == null) {
                        addStatement("else·-> throw·%T(%P)", jsonDataException, "Expected one of ${enumToJsonName.values} but was \${${readerParameter.name}.nextString()} at path \${${readerParameter.name}.path}")
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