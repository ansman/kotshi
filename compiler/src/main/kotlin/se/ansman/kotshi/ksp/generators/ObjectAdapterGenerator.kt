package se.ansman.kotshi.ksp.generators

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.jvm.throws
import se.ansman.kotshi.GlobalConfig
import se.ansman.kotshi.addControlFlow
import se.ansman.kotshi.addElse
import se.ansman.kotshi.addIfElse
import se.ansman.kotshi.nullable

class ObjectAdapterGenerator(
    environment: SymbolProcessorEnvironment,
    resolver: Resolver,
    element: KSClassDeclaration,
    globalConfig: GlobalConfig
) : AdapterGenerator(environment, resolver, element, globalConfig) {
    init {
        require(element.classKind == ClassKind.OBJECT)
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