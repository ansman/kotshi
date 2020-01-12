package se.ansman.kotshi

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.jvm.throws
import com.squareup.kotlinpoet.metadata.ImmutableKmClass
import com.squareup.kotlinpoet.metadata.isEnum
import com.squareup.kotlinpoet.metadata.specs.ClassInspector
import com.squareup.moshi.JsonReader
import java.io.IOException
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements

class EnumAdapterGenerator(
    classInspector: ClassInspector,
    elements: Elements,
    element: TypeElement,
    metadata: ImmutableKmClass,
    globalConfig: GlobalConfig
) : AdapterGenerator(classInspector, elements, element, metadata, globalConfig) {
    init {
        require(metadata.isEnum)
    }

    override fun TypeSpec.Builder.addMethods(): Collection<String> {
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
                .throws(IOException::class.java)
                .addParameter(writer)
                .addParameter(value)
                .addWhen("%N", value) {
                    for ((entry, name) in enumToJsonName) {
                        addStatement("%T.%N·-> %N.value(%S)", className, entry, writer, name)
                    }
                    addStatement("null·-> %N.nullValue()", writer)
                }
                .build())
            .addFunction(FunSpec.builder("fromJson")
                .addModifiers(KModifier.OVERRIDE)
                .throws(IOException::class.java)
                .addParameter(reader)
                .returns(typeName.nullable())
                .addControlFlow("return·if (%N.peek() == %T.NULL)", reader, JsonReader.Token::class.java, close = false) {
                    addStatement("%N.nextNull()", reader)
                }
                .addNextControlFlow("else when (%N.selectString(options))", reader) {
                    enumToJsonName.keys.forEachIndexed { index, entry ->
                        addStatement("$index·-> %T.%N", className, entry)
                    }
                    if (defaultValue == null) {
                        addStatement("else·-> throw·%T(%P)", jsonDataException, "Expected one of ${enumToJsonName.values} but was \${${reader.name}.nextString()} at path \${${reader.name}.path}")
                    } else {
                        addControlFlow("else·->") {
                            addStatement("%N.skipValue()", reader)
                            addStatement("%T.%N", className, defaultValue)
                        }
                    }
                }
                .build())
        return enumToJsonName.values
    }
}