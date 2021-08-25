package se.ansman.kotshi.renderer

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeSpec
import se.ansman.kotshi.Types
import se.ansman.kotshi.Types.Moshi.jsonDataException
import se.ansman.kotshi.addControlFlow
import se.ansman.kotshi.addNextControlFlow
import se.ansman.kotshi.addWhen
import se.ansman.kotshi.model.EnumJsonAdapter

class EnumAdapterRenderer(private val adapter: EnumJsonAdapter) : AdapterRenderer(adapter) {
    private val optionsProperty = jsonOptionsProperty(adapter.entries.map { it.serializedName })
        
    override fun TypeSpec.Builder.renderSetup() {
        addProperty(optionsProperty)
    }

    override fun FunSpec.Builder.renderToJson(
        writerParameter: ParameterSpec,
        valueParameter: ParameterSpec
    ) {
        addWhen("%N", valueParameter) {
            for (entry in adapter.entries) {
                addStatement("%T.%N·-> %N.value(%S)", adapter.targetType, entry.name, writerParameter, entry.serializedName)
            }
            addStatement("null·-> %N.nullValue()", writerParameter)
        }
    }

    override fun FunSpec.Builder.renderFromJson(readerParameter: ParameterSpec) {
        this
            .addControlFlow("return·if (%N.peek() == %T.NULL)", readerParameter, Types.Moshi.jsonReaderToken, close = false) {
                addStatement("%N.nextNull()", readerParameter)
            }
            .addNextControlFlow("else when (%N.selectString(%N))",
                readerParameter,
                optionsProperty
            ) {
                adapter.entries.forEachIndexed { index, entry ->
                    addStatement("$index·-> %T.%N", adapter.targetType, entry.name)
                }
                if (adapter.fallback == null) {
                    addStatement(
                        "else·-> throw·%T(%P)",
                        jsonDataException,
                        "Expected one of ${adapter.entries.map { it.serializedName }} but was \${${readerParameter.name}.nextString()} at path \${${readerParameter.name}.path}"
                    )
                } else {
                    addControlFlow("else·->") {
                        addStatement("%N.skipValue()", readerParameter)
                        addStatement("%T.%N", adapter.targetType, adapter.fallback.name)
                    }
                }
            }
    }
}