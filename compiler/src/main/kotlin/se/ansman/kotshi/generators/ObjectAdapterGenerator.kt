package se.ansman.kotshi.generators

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.jvm.throws
import com.squareup.kotlinpoet.metadata.ImmutableKmClass
import com.squareup.kotlinpoet.metadata.isObject
import com.squareup.kotlinpoet.metadata.specs.ClassInspector
import se.ansman.kotshi.addControlFlow
import se.ansman.kotshi.addElse
import se.ansman.kotshi.addIfElse
import se.ansman.kotshi.addNextControlFlow
import se.ansman.kotshi.addWhile
import se.ansman.kotshi.nullable
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements

class ObjectAdapterGenerator(
    classInspector: ClassInspector,
    elements: Elements,
    element: TypeElement,
    metadata: ImmutableKmClass,
    globalConfig: GlobalConfig
) : AdapterGenerator(classInspector, elements, element, metadata, globalConfig) {
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
                    addStatement("%N.beginObject().endObject()", writerParameter)
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
                .addNextControlFlow("else") {
                    addStatement("%N.beginObject()", readerParameter)
                    addWhile("%N.hasNext()", readerParameter) {
                        addStatement("%N.skipName()", readerParameter)
                        addStatement("%N.skipValue()", readerParameter)
                    }
                    addStatement("%N.endObject()", readerParameter)
                    addStatement("%T", typeName)
                }
                .build())
    }
}