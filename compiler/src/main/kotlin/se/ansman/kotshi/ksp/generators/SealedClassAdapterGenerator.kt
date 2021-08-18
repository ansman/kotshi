package se.ansman.kotshi.ksp.generators

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.Modifier.SEALED
import com.squareup.kotlinpoet.ARRAY
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.plusParameter
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.jvm.throws
import se.ansman.kotshi.GlobalConfig
import se.ansman.kotshi.JsonDefaultValue
import se.ansman.kotshi.Polymorphic
import se.ansman.kotshi.Polymorphic.Fallback
import se.ansman.kotshi.PolymorphicLabel
import se.ansman.kotshi.addControlFlow
import se.ansman.kotshi.addElse
import se.ansman.kotshi.addIf
import se.ansman.kotshi.addIfElse
import se.ansman.kotshi.addWhile
import se.ansman.kotshi.applyEachIndexed
import se.ansman.kotshi.applyIf
import se.ansman.kotshi.kapt.generators.ioException
import se.ansman.kotshi.kapt.generators.jsonAdapter
import se.ansman.kotshi.kapt.generators.jsonDataException
import se.ansman.kotshi.kapt.generators.jsonReaderOptions
import se.ansman.kotshi.kapt.generators.jsonReaderToken
import se.ansman.kotshi.kapt.generators.moshiParameter
import se.ansman.kotshi.kapt.generators.readerParameter
import se.ansman.kotshi.kapt.generators.typesParameter
import se.ansman.kotshi.kapt.generators.writerParameter
import se.ansman.kotshi.ksp.KspProcessingError
import se.ansman.kotshi.ksp.SealedClassSubtype
import se.ansman.kotshi.ksp.getAnnotation
import se.ansman.kotshi.ksp.getEnumValue
import se.ansman.kotshi.ksp.getValue
import se.ansman.kotshi.ksp.toClassName
import se.ansman.kotshi.ksp.toTypeName
import se.ansman.kotshi.nullable

class SealedClassAdapterGenerator(
    environment: SymbolProcessorEnvironment,
    resolver: Resolver,
    element: KSClassDeclaration,
    globalConfig: GlobalConfig
) : AdapterGenerator(environment, resolver, element, globalConfig) {
    init {
        require(SEALED in element.modifiers)

        nameAllocator.newName("peek")
        nameAllocator.newName("labelIndex")
        nameAllocator.newName("adapter")
    }

    private val annotation = element.getAnnotation(Polymorphic::class.java)
        ?: throw KspProcessingError("Sealed classes must be annotated with @Polymorphic", element)

    private val labelKey = annotation.getValue<String>("labelKey")

    private val labelKeyOptions = PropertySpec
        .builder(nameAllocator.newName("labelKeyOptions"), jsonReaderOptions, KModifier.PRIVATE)
        .initializer("%T.of(%S)", jsonReaderOptions, labelKey)
        .build()

    override fun TypeSpec.Builder.addMethods() {
        val implementations = element.getAllSealedSubclasses().toList()

        val subtypes = implementations.mapNotNull {
            SealedClassSubtype(
                type = it,
                label = it.getAnnotation(PolymorphicLabel::class.java)
                    ?.getValue("value")
                    ?: run {
                        if (SEALED !in it.modifiers && it.getAnnotation<JsonDefaultValue>() == null) {
                            throw KspProcessingError("Missing @PolymorphicLabel on ${it.toClassName()}", it)
                        }
                        return@mapNotNull null
                    }
            )
        }

        if (subtypes.isEmpty()) {
            throw KspProcessingError("No classes annotated with @PolymorphicLabel", element)
        }

        val labels = subtypes.map { it.label }

        for ((label, types) in subtypes.groupBy { it.label }.entries) {
            if (types.size != 1) {
                throw KspProcessingError("@PolymorphicLabel $label found on multiple classes", types.first().type)
            }
        }

        val defaultType = implementations
            .filter { it.getAnnotation(JsonDefaultValue::class.java) != null }
            .let {
                when (it.size) {
                    0 -> null
                    1 -> it.single()
                    else -> throw KspProcessingError("Multiple classes annotated with @JsonDefaultValue", it.first())
                }
            }

        val onMissing = annotation.getEnumValue("onMissing", Fallback.DEFAULT)
        val onInvalid = annotation.getEnumValue("onInvalid", Fallback.DEFAULT)
        if (defaultType != null && onMissing != Fallback.DEFAULT && onInvalid != Fallback.DEFAULT) {
            throw KspProcessingError(
                "@JsonDefaultValue cannot be used in combination with onMissing=$onMissing and onInvalid=$onInvalid",
                defaultType
            )
        }

        val adapterType = jsonAdapter.plusParameter(typeName)
        val adapters =
            PropertySpec.builder(nameAllocator.newName("adapters"), ARRAY.plusParameter(adapterType), KModifier.PRIVATE)
                .initializer(CodeBlock.builder()
                    .add("arrayOf(")
                    .indent()
                    .applyEachIndexed(subtypes) { index, subtype ->
                        if (index > 0) {
                            add(",")
                        }
                        add("\n%N.adapter<%T>(", moshiParameter, typeName)

                        add(
                            subtype.render(
                                typeName = subtype.type.toTypeName(),
                                forceBox = true
                            )
                        )
                        add(")")
                    }
                    .unindent()
                    .add("\n)\n")
                    .build())
                .build()

        val defaultAdapter = if (defaultType == null) {
            null
        } else {
            val defaultIndex = subtypes.indexOfFirst { it.type == defaultType }
            if (defaultIndex == -1) {
                val defaultAdapter =
                    PropertySpec.builder(nameAllocator.newName("defaultAdapter"), adapterType, KModifier.PRIVATE)
                        .initializer("moshi.adapter<%T>(%T::class.java)", typeName, defaultType.toClassName())
                        .build()
                addProperty(defaultAdapter)
                CodeBlock.of("%N", defaultAdapter)
            } else {
                CodeBlock.of("adapters[%L]", defaultIndex)
            }
        }

        this
            .primaryConstructor(FunSpec.constructorBuilder()
                .addParameter(moshiParameter)
                .applyIf(typeVariables.isNotEmpty()) {
                    addParameter(typesParameter)
                }
                .build())
            .addProperty(adapters)
            .addFunction(FunSpec.builder("toJson")
                .addModifiers(KModifier.OVERRIDE)
                .throws(ioException)
                .addParameter(writerParameter)
                .addParameter(value)
                .addIfElse("%N == null", value) {
                    addStatement("%N.nullValue()", writerParameter)
                }
                .addElse {
                    addControlFlow("val adapter = when (%N)", value) {
                        subtypes.forEachIndexed { index, subtype ->
                            val generics = subtype.type.typeParameters.map { "*" }
                                .takeIf { it.isNotEmpty() }
                                ?.joinToString(", ", prefix = "<", postfix = ">")
                                ?: ""
                            addStatement("is %T%L·-> %N[%L]", subtype.className, generics, adapters, index)
                        }
                        if (defaultAdapter != null && defaultType != null && subtypes.none { it.type == defaultType }) {
                            addStatement(
                                "is %T·-> %L",
                                defaultType.toTypeName(defaultType.typeParameters.map { STAR }),
                                defaultAdapter
                            )
                        }
                    }
                    addStatement("adapter.toJson(%N, %N)", writerParameter, value)
                }
                .build())
            .addFunction(FunSpec.builder("fromJson")
                .addModifiers(KModifier.OVERRIDE)
                .throws(ioException)
                .addParameter(readerParameter)
                .returns(typeName.nullable())
                .addControlFlow(
                    "return·if·(%N.peek()·==·%T.NULL)",
                    readerParameter,
                    jsonReaderToken,
                    close = false
                ) {
                    addStatement("%N.nextNull()", readerParameter)
                }
                .addElse {
                    addControlFlow("%N.peekJson().use·{·peek·->", readerParameter) {
                        addStatement("peek.setFailOnUnknown(false)")
                        addStatement("peek.beginObject()")
                        addWhile("peek.hasNext()") {
                            addIf("peek.selectName(%N)·==·-1", labelKeyOptions) {
                                addStatement("peek.skipName()")
                                addStatement("peek.skipValue()")
                                addStatement("continue")
                            }
                            addStatement("val·labelIndex·= peek.selectString(options)")
                            addControlFlow("val·adapter·= if·(labelIndex·==·-1)", close = false) {
                                if (onInvalid == Fallback.FAIL || defaultType == null && onInvalid == Fallback.DEFAULT) {
                                    addStatement(
                                        "throw·%T(%S·+ peek.nextString())",
                                        jsonDataException,
                                        "Expected one of $labels for key '$labelKey' but found "
                                    )
                                } else if (onInvalid == Fallback.NULL) {
                                    addStatement("%N.skipValue()", readerParameter)
                                    addStatement("return·null")
                                } else {
                                    addStatement("%L", defaultAdapter ?: throw AssertionError("Unhandled case"))
                                }
                            }
                            addElse {
                                addStatement("adapters[labelIndex]")
                            }
                            addStatement("return·adapter.fromJson(%N)", readerParameter)
                        }

                        if (onMissing == Fallback.FAIL || defaultType == null && onMissing == Fallback.DEFAULT) {
                            addStatement("throw·%T(%S)", jsonDataException, "Missing label for $labelKey")
                        } else if (onMissing == Fallback.NULL) {
                            addStatement("%N.skipValue()", readerParameter)
                            addStatement("null")
                        } else {
                            addStatement(
                                "%L.fromJson(%N)",
                                defaultAdapter ?: throw AssertionError("Unhandled case"),
                                readerParameter
                            )
                        }
                    }
                }
                .build())
            .addOptions(labels)
            .addProperty(labelKeyOptions)
    }

    private fun KSClassDeclaration.getAllSealedSubclasses(): Sequence<KSClassDeclaration> =
        getSealedSubclasses().flatMap { subclass ->
            if (SEALED in subclass.modifiers) {
                sequenceOf(subclass) + subclass.getAllSealedSubclasses()
            } else {
                sequenceOf(subclass)
            }
        }
}