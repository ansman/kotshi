package se.ansman.kotshi

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.jvm.throws
import com.squareup.kotlinpoet.metadata.ImmutableKmClass
import com.squareup.kotlinpoet.metadata.isObject
import com.squareup.kotlinpoet.metadata.specs.ClassInspector
import com.squareup.moshi.JsonReader
import java.io.IOException
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
                .throws(IOException::class.java)
                .addParameter(writer)
                .addParameter(value)
                .addIfElse("%N == null", value) {
                    addStatement("%N.nullValue()", writer)
                }
                .addElse {
                    addStatement("%N.beginObject().endObject()", writer)
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
                .addNextControlFlow("else") {
                    addStatement("%N.beginObject()", reader)
                    addWhile("%N.hasNext()", reader) {
                        addStatement("%N.skipName()", reader)
                        addStatement("%N.skipValue()", reader)
                    }
                    addStatement("%N.endObject()", reader)
                    addStatement("%T", typeName)
                }
                .build())
    }
}