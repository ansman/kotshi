package se.ansman.kotshi.renderer

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import se.ansman.kotshi.Types.Moshi.jsonDataException
import se.ansman.kotshi.Types.Moshi.jsonReaderToken
import se.ansman.kotshi.addControlFlow
import se.ansman.kotshi.addIfElse
import se.ansman.kotshi.addNextControlFlow
import se.ansman.kotshi.model.ObjectJsonAdapter

class ObjectAdapterRenderer(private val adapter: ObjectJsonAdapter) : AdapterRenderer(adapter) {
    override fun FunSpec.Builder.renderFromJson(readerParameter: ParameterSpec) {
        addControlFlow("return when·(%N.peek())", readerParameter) {
            addStatement("%T.NULL·-> %N.nextNull()", jsonReaderToken, readerParameter)
            addControlFlow("%T.BEGIN_OBJECT·->", jsonReaderToken) {
                addStatement("%N.skipValue()", readerParameter)
                addStatement("%T", adapter.targetType)
            }
            addStatement("else·-> throw·%T(%P)", jsonDataException, "Expected BEGIN_OBJECT but was \${${readerParameter.name}.peek()} at path \${${readerParameter.name}.path}")
        }
    }

    override fun FunSpec.Builder.renderToJson(
        writerParameter: ParameterSpec,
        valueParameter: ParameterSpec
    ) {
        this
            .addIfElse("%N·==·null", valueParameter) {
                addStatement("%N.nullValue()", writerParameter)
            }
            .addNextControlFlow("else·with(%N)", writerParameter) {
                addStatement("beginObject()")
                for ((key, value) in adapter.polymorphicLabels) {
                    addStatement("name(%S).value(%S)", key, value)
                }
                addStatement("endObject()")
            }
    }

}