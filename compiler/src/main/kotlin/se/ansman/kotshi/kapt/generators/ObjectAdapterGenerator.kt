package se.ansman.kotshi.kapt.generators

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.jvm.throws
import com.squareup.kotlinpoet.metadata.ImmutableKmClass
import com.squareup.kotlinpoet.metadata.isObject
import se.ansman.kotshi.GlobalConfig
import se.ansman.kotshi.kapt.MetadataAccessor
import se.ansman.kotshi.addControlFlow
import se.ansman.kotshi.addElse
import se.ansman.kotshi.addIfElse
import se.ansman.kotshi.nullable
import javax.annotation.processing.Messager
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

class ObjectAdapterGenerator(
    metadataAccessor: MetadataAccessor,
    types: Types,
    elements: Elements,
    element: TypeElement,
    metadata: ImmutableKmClass,
    globalConfig: GlobalConfig,
    messager: Messager
) : AdapterGenerator(metadataAccessor, types, elements, element, metadata, globalConfig) {
    init {
        require(metadata.isObject)
    }

    override fun TypeSpec.Builder.addMethods() {
        this
            .addFunction(FunSpec.builder("toJson")
                .addModifiers(KModifier.OVERRIDE)
                .throws(ioException)
                .addParameter(writerParameter)
                .addParameter(value)
                .addIfElse("%N == null", value) {
                    addStatement("%N.nullValue()", writerParameter)
                }
                .addElse {
                    addStatement("%N.beginObject()", writerParameter)
                    for ((key, value) in getPolymorphicLabels()) {
                        addStatement("%N.name(%S).value(%S)", writerParameter, key, value)
                    }
                    addStatement("%N.endObject()", writerParameter)
                }
                .build())
            .addFunction(FunSpec.builder("fromJson")
                .addModifiers(KModifier.OVERRIDE)
                .throws(ioException)
                .addParameter(readerParameter)
                .returns(typeName.nullable())
                .addControlFlow("return when·(%N.peek())", readerParameter) {
                    addStatement("%T.NULL·-> %N.nextNull()", jsonReaderToken, readerParameter)
                    addControlFlow("%T.BEGIN_OBJECT·->", jsonReaderToken) {
                        addStatement("%N.skipValue()", readerParameter)
                        addStatement("%T", typeName)
                    }
                    addStatement("else·-> throw·%T(%P)", jsonDataException, "Expected BEGIN_OBJECT but was \${${readerParameter.name}.peek()} at path \${${readerParameter.name}.path}")
                }
                .build())
    }
}