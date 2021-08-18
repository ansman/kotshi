package se.ansman.kotshi.ksp.generators

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueArgument
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.BOOLEAN_ARRAY
import com.squareup.kotlinpoet.BYTE
import com.squareup.kotlinpoet.BYTE_ARRAY
import com.squareup.kotlinpoet.CHAR
import com.squareup.kotlinpoet.CHAR_ARRAY
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.DOUBLE_ARRAY
import com.squareup.kotlinpoet.Dynamic
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.FLOAT_ARRAY
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.INT_ARRAY
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.LONG_ARRAY
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.plusParameter
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.SHORT_ARRAY
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.jvm.throws
import com.squareup.kotlinpoet.tag
import se.ansman.kotshi.AdapterKey
import se.ansman.kotshi.GlobalConfig
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.Property
import se.ansman.kotshi.STRING
import se.ansman.kotshi.SerializeNulls
import se.ansman.kotshi.addControlFlow
import se.ansman.kotshi.addElse
import se.ansman.kotshi.addIf
import se.ansman.kotshi.addIfElse
import se.ansman.kotshi.addWhen
import se.ansman.kotshi.addWhenBranch
import se.ansman.kotshi.addWhile
import se.ansman.kotshi.applyEach
import se.ansman.kotshi.applyEachIndexed
import se.ansman.kotshi.applyIf
import se.ansman.kotshi.asRuntimeType
import se.ansman.kotshi.isPrimitive
import se.ansman.kotshi.kapt.generators.ioException
import se.ansman.kotshi.kapt.generators.jsonAdapter
import se.ansman.kotshi.kapt.generators.jsonDataException
import se.ansman.kotshi.kapt.generators.jsonReaderToken
import se.ansman.kotshi.kapt.generators.kotshiUtilsAppendNullableError
import se.ansman.kotshi.kapt.generators.kotshiUtilsByteValue
import se.ansman.kotshi.kapt.generators.kotshiUtilsCreateJsonQualifierImplementation
import se.ansman.kotshi.kapt.generators.kotshiUtilsNextByte
import se.ansman.kotshi.kapt.generators.kotshiUtilsNextChar
import se.ansman.kotshi.kapt.generators.kotshiUtilsNextFloat
import se.ansman.kotshi.kapt.generators.kotshiUtilsNextShort
import se.ansman.kotshi.kapt.generators.kotshiUtilsValue
import se.ansman.kotshi.kapt.generators.moshiParameter
import se.ansman.kotshi.kapt.generators.readerParameter
import se.ansman.kotshi.kapt.generators.typesParameter
import se.ansman.kotshi.kapt.generators.writerParameter
import se.ansman.kotshi.ksp.KspProcessingError
import se.ansman.kotshi.ksp.getAnnotation
import se.ansman.kotshi.ksp.getEnumValue
import se.ansman.kotshi.ksp.toTypeName
import se.ansman.kotshi.notNull
import se.ansman.kotshi.nullable
import se.ansman.kotshi.suggestedAdapterName

class DataClassAdapterGenerator(
    environment: SymbolProcessorEnvironment,
    resolver: Resolver,
    element: KSClassDeclaration,
    globalConfig: GlobalConfig
) : AdapterGenerator(environment, resolver, element, globalConfig) {


    init {
        require(Modifier.DATA in element.modifiers)
    }

    private val serializeNulls = element.getAnnotation<JsonSerializable>()!!
        .getEnumValue("serializeNulls", SerializeNulls.DEFAULT)
        .takeUnless { it == SerializeNulls.DEFAULT }
        ?: globalConfig.serializeNulls

    override fun TypeSpec.Builder.addMethods() {
        val properties = element.primaryConstructor!!.parameters.map { parameter ->
            Property.create(
                globalConfig = globalConfig,
                resolver = resolver,
                enclosingClass = element,
                parameter = parameter,
            )
        }
        if (properties.isEmpty()) {
            throw KspProcessingError("Could not find any data class properties.", element)
        }

        val adapterKeys = properties
            .asSequence()
            .filter { it.shouldUseAdapter }
            .map { it.adapterKey }
            .distinct()
            .generatePropertySpecs()

        primaryConstructor(FunSpec.constructorBuilder()
            .applyIf(adapterKeys.isNotEmpty()) {
                addParameter(moshiParameter)
            }
            .applyIf(adapterKeys.any { it.key.type.hasTypeVariable }) {
                addParameter(typesParameter)
            }
            .build())
            .addProperties(adapterKeys.values)
            .addToJson(properties, adapterKeys)
            .addFromJson(properties, adapterKeys)
            .build()

        maybeAddCompanion(properties
            .asSequence()
            .filterNot { it.isTransient }
            .map { it.jsonName }
            .toList())
    }

    private fun Sequence<AdapterKey>.generatePropertySpecs(): Map<AdapterKey, PropertySpec> {
        fun AdapterKey.annotations(): CodeBlock = when {
            jsonQualifiers.isEmpty() -> CodeBlock.of("")
            jsonQualifiers.singleOrNull()?.members?.isEmpty() == true ->
                CodeBlock.of(", %T::class.java", jsonQualifiers.single().className)
            else -> CodeBlock.builder()
                .add(", setOf(")
                .applyIf(jsonQualifiers.size > 1) { add("⇥\n") }
                .applyEachIndexed(jsonQualifiers) { index, qualifier ->
                    if (index > 0) add(",\n")
                    add(resolver, qualifier.tag<KSAnnotation>()!!)
                }
                .applyIf(jsonQualifiers.size > 1) { add("⇤\n") }
                .add(")")
                .build()
        }

        return associateWith { adapterKey ->
            PropertySpec
                .builder(
                    nameAllocator.newName(adapterKey.suggestedAdapterName),
                    jsonAdapter.plusParameter(adapterKey.type),
                    KModifier.PRIVATE
                )
                .initializer(
                    CodeBlock.builder()
                        .add("«%N.adapter(", moshiParameter)
                        .add(adapterKey.asRuntimeType { typeVariableName ->
                            val genericIndex = typeVariables.indexOfFirst { it.name == typeVariableName.name }
                            if (genericIndex == -1) {
                                throw KspProcessingError("Element is generic but is of an unknown type", element)
                            } else {
                                CodeBlock.of("%N[$genericIndex]", typesParameter)
                            }
                        })
                        .add(adapterKey.annotations())
                        .add(")»")
                        .build()
                )
                .build()
        }
    }

    private fun TypeSpec.Builder.addToJson(
        properties: Collection<Property>,
        adapterKeys: Map<AdapterKey, PropertySpec>
    ): TypeSpec.Builder {
        val builder = FunSpec.builder("toJson")
            .addModifiers(KModifier.OVERRIDE)
            .throws(ioException)
            .addParameter(writerParameter)
            .addParameter(value)

        val propertyNames = properties.mapTo(mutableSetOf()) { it.jsonName }
        val labels = getPolymorphicLabels().filterKeys { it !in propertyNames }

        fun FunSpec.Builder.addBody(): FunSpec.Builder =
            addStatement("%N.beginObject()", writerParameter)
                .applyEach(labels.entries) { (key, value) ->
                    addStatement("%N.name(%S).value(%S)", writerParameter, key, value)
                }
                .applyEach(properties.filterNot { it.isTransient }) { property ->
                    addStatement("%N.name(%S)", writerParameter, property.jsonName)
                    val getter = CodeBlock.of("%N.%L", value, property.name)

                    if (property.shouldUseAdapter) {
                        addCode("%N.toJson(%N, ", adapterKeys.getValue(property.adapterKey), writerParameter).addCode(
                            getter
                        ).addCode(")\n")
                    } else when (property.type.notNull()) {
                        STRING,
                        INT,
                        LONG,
                        FLOAT,
                        DOUBLE,
                        SHORT,
                        BOOLEAN -> addStatement("%N.value(%L)", writerParameter, getter)
                        BYTE -> addStatement("%N.%M(%L)", writerParameter, kotshiUtilsByteValue, getter)
                        CHAR -> addStatement("%N.%M(%L)", writerParameter, kotshiUtilsValue, getter)
                        else -> throw KspProcessingError(
                            "Property ${property.name} is not primitive ${property.type} but requested non adapter use",
                            element
                        )
                    }
                }
                .addStatement("%N.endObject()", writerParameter)

        builder
            .addIf("%N == null", value) {
                addStatement("%N.nullValue()", writerParameter)
                addStatement("return")
            }

        val serializeNullsEnabled = when (serializeNulls) {
            SerializeNulls.DEFAULT -> null
            SerializeNulls.ENABLED -> true
            SerializeNulls.DISABLED -> false
        }
        if (serializeNullsEnabled != null) {
            builder
                .addStatement("val·serializeNulls·= %N.serializeNulls", writerParameter)
                .addStatement("%N.serializeNulls·= %L", writerParameter, serializeNullsEnabled)
                .beginControlFlow("try")
                .addBody()
                .nextControlFlow("finally")
                .addStatement("%N.serializeNulls·= serializeNulls", writerParameter)
                .endControlFlow()
        } else {
            builder.addBody()
        }
        return addFunction(builder.build())
    }

    private fun TypeSpec.Builder.addFromJson(
        properties: Collection<Property>,
        adapters: Map<AdapterKey, PropertySpec>
    ): TypeSpec.Builder {
        val builder = FunSpec.builder("fromJson")
            .addModifiers(KModifier.OVERRIDE)
            .throws(ioException)
            .addParameter(readerParameter)
            .returns(typeName.nullable())

        val variables = properties
            .asSequence()
            .filterNot { it.isTransient }
            .associateBy({ it }, { it.createVariables() })

        return addFunction(builder
            .addStatement(
                "if (%N.peek() == %T.NULL) return %N.nextNull()",
                readerParameter,
                jsonReaderToken,
                readerParameter
            )
            .addCode("\n")
            .applyEach(variables.values) { variable ->
                addCode("%L", variable.value)
                if (variable.helper != null) {
                    addCode("%L", variable.helper)
                }
            }
            .addCode("\n")
            .addStatement("%N.beginObject()", readerParameter)
            .addWhile("%N.hasNext()", readerParameter) {
                addWhen("%N.selectName(options)", readerParameter) {
                    variables.entries.forEachIndexed { index, (property, variable) ->
                        addWhenBranch("%L", index) {
                            if (property.shouldUseAdapter) {
                                addStatement(
                                    "%N·= %N.fromJson(%N)",
                                    variable.value,
                                    adapters.getValue(property.adapterKey),
                                    readerParameter
                                )
                                if (variable.helper != null) {
                                    if (property.type.isNullable) {
                                        addStatement("%N·= true", variable.helper)
                                    } else {
                                        addStatement("?.also { %N = true }", variable.helper)
                                    }
                                }
                            } else {
                                fun FunSpec.Builder.readPrimitive(functionName: String, vararg args: Any) {
                                    addIfElse("%N.peek() == %T.NULL", readerParameter, jsonReaderToken) {
                                        addStatement("%N.skipValue()", readerParameter)
                                    }
                                    addElse {
                                        addStatement(
                                            "%N·= %N.$functionName()",
                                            variable.value,
                                            readerParameter,
                                            *args
                                        )
                                        if (variable.helper != null && !property.type.isNullable) {
                                            addStatement("%N·= true", variable.helper)
                                        }
                                    }
                                    if (variable.helper != null && property.type.isNullable) {
                                        addStatement("%N·= true", variable.helper)
                                    }
                                }

                                when (property.type.notNull()) {
                                    STRING -> readPrimitive("nextString")
                                    BOOLEAN -> readPrimitive("nextBoolean")
                                    BYTE -> readPrimitive("%M", kotshiUtilsNextByte)
                                    SHORT -> readPrimitive("%M", kotshiUtilsNextShort)
                                    INT -> readPrimitive("nextInt")
                                    LONG -> readPrimitive("nextLong")
                                    CHAR -> readPrimitive("%M", kotshiUtilsNextChar)
                                    FLOAT -> readPrimitive("%M", kotshiUtilsNextFloat)
                                    DOUBLE -> readPrimitive("nextDouble")
                                    else -> throw KspProcessingError(
                                        "Internal Kotshi error. Expected property type to be a primitive but was ${property.type}. Please open an issue here: https://github.com/ansman/kotshi/issues/new",
                                        element
                                    )
                                }
                            }
                        }
                    }
                    addWhenBranch("-1") {
                        addStatement("%N.skipName()", readerParameter)
                        addStatement("%N.skipValue()", readerParameter)
                    }
                }
            }
            .addStatement("%N.endObject()", readerParameter)
            .addCode("\n")
            .apply {
                val propertiesToCheck = variables.entries
                    .filter { (property, _) ->
                        !property.type.isNullable && !property.hasDefaultValue
                    }

                if (propertiesToCheck.isNotEmpty()) {
                    val stringBuilder = PropertySpec
                        .builder("errorBuilder", StringBuilder::class.asTypeName().nullable())
                        .mutable()
                        .initializer("null")
                        .build()
                    addCode("%L", stringBuilder)
                    for ((property, variable) in propertiesToCheck) {
                        addIf("%L", variable.isNotSet) {
                            if (property.name == property.jsonName) {
                                addStatement(
                                    "%N = %N.%M(%S)",
                                    stringBuilder,
                                    stringBuilder,
                                    kotshiUtilsAppendNullableError,
                                    property.name
                                )
                            } else {
                                addStatement(
                                    "%N = %N.%M(%S, %S)",
                                    stringBuilder,
                                    stringBuilder,
                                    kotshiUtilsAppendNullableError,
                                    property.name,
                                    property.jsonName
                                )
                            }
                        }
                    }

                    addIf("%N != null", stringBuilder) {
                        addStatement(
                            "%N.append(\" (at path \").append(%N.path).append(')')",
                            stringBuilder,
                            readerParameter
                        )
                        addStatement("throw %T(%N.toString())", jsonDataException, stringBuilder)
                    }
                    addCode("\n")
                }
            }
            .apply {
                val constructorProperties = properties.filter {
                    !it.hasDefaultValue
                }
                val copyProperties = properties.filter {
                    it.hasDefaultValue && !it.isTransient
                }

                addCode("return·%T(«", typeName)
                constructorProperties.forEachIndexed { index, property ->
                    val variable = variables.getValue(property)

                    if (index > 0) {
                        addCode(",")
                    }
                    addCode("\n%N·= %N", property.name, variable.value.name)
                    if (variable.value.type.isNullable && !property.type.isNullable) {
                        addCode("!!")
                    }
                }
                addCode("»")
                if (constructorProperties.isNotEmpty()) addCode("\n")
                addCode(")")
                if (copyProperties.isNotEmpty()) {
                    addControlFlow(".let") {
                        addCode("it.copy(«")
                        copyProperties.forEachIndexed { index, property ->
                            val variable = variables.getValue(property)

                            if (index > 0) {
                                addCode(",")
                            }
                            addCode("\n%N = ", property.name)

                            if (variable.helper == null) {
                                addCode("%N ?: it.%N", variable.value, property.name)
                            } else {
                                addCode("if (%L) %N else it.%N", variable.isSet, variable.value, property.name)
                            }
                        }
                        addCode("»\n)\n")
                    }
                }
            }
            .build())
    }

    private fun Property.createVariables() =
        PropertyVariables(
            value = PropertySpec
                .builder(
                    name = nameAllocator.newName(name),
                    type = if (type.isPrimitive && !shouldUseAdapter) type else type.nullable()
                )
                .initializer(
                    when {
                        type.isPrimitive && !shouldUseAdapter ->
                            when (type) {
                                BOOLEAN -> CodeBlock.of("false")
                                BYTE -> CodeBlock.of("0")
                                SHORT -> CodeBlock.of("0")
                                INT -> CodeBlock.of("0")
                                LONG -> CodeBlock.of("0L")
                                CHAR -> CodeBlock.of("'\\u0000'")
                                FLOAT -> CodeBlock.of("0f")
                                DOUBLE -> CodeBlock.of("0.0")
                                else -> throw AssertionError()
                            }
                        else -> CodeBlock.of("null")
                    }
                )
                .mutable()
                .build(),
            helper = if (type.isPrimitive && !shouldUseAdapter || type.isNullable && hasDefaultValue) {
                PropertySpec
                    .builder(nameAllocator.newName("${name}IsSet"), BOOLEAN)
                    .mutable()
                    .initializer("false")
                    .build()
            } else {
                null
            }
        )
}

private data class PropertyVariables(
    val value: PropertySpec,
    val helper: PropertySpec?
) {
    val isNotSet: CodeBlock by lazy(LazyThreadSafetyMode.NONE) {
        if (helper == null) {
            CodeBlock.of("%N == null", value)
        } else {
            CodeBlock.of("!%N", helper)
        }
    }

    val isSet: CodeBlock by lazy(LazyThreadSafetyMode.NONE) {
        if (helper == null) {
            CodeBlock.of("%N != null", value)
        } else {
            CodeBlock.of("%N", helper)
        }
    }
}

@OptIn(ExperimentalStdlibApi::class)
private fun CodeBlock.Builder.add(resolver: Resolver, value: Any, type: KSType): CodeBlock.Builder = apply {
    when (value) {
        is KSType -> {
            if (Modifier.ENUM in type.declaration.modifiers) {
                add("%T", value.toTypeName())
            } else {
                add("%T::class.java", value.toTypeName())
            }
        }
        is KSName ->
            add(
                "%T.%L", ClassName.bestGuess(value.getQualifier()),
                value.getShortName()
            )
        is KSAnnotation -> add(resolver, value)
        else -> add(memberForValue(resolver, value, type))
    }
}

private fun CodeBlock.Builder.add(resolver: Resolver, annotation: KSAnnotation): CodeBlock.Builder = apply {
    val annotationType = annotation.annotationType.resolve()
    if (annotation.arguments.isEmpty()) {
        add("%T::class.java.%M()", annotationType.toTypeName(), kotshiUtilsCreateJsonQualifierImplementation)
    } else {
        add(
            "%T::class.java.%M(mapOf(⇥",
            annotationType.toTypeName(),
            kotshiUtilsCreateJsonQualifierImplementation
        )

        val typeByName = annotation.annotationType.resolve().declaration.let { it as KSClassDeclaration }
            .primaryConstructor
            ?.parameters
            ?.associateBy({ it.name!! }, { it.type })
            ?: emptyMap()
        annotation.arguments.forEachIndexed { i, element ->
            if (i > 0) {
                add(",")
            }
            add("\n")
            val value = element.value
            if (value != null) {
                add("%S·to·", element.name!!.asString())
                add(resolver, value, typeByName.getValue(element.name!!).resolve())
            }
            add("")
        }
        add("⇤\n))")
    }
}

private val TypeName.hasTypeVariable: Boolean
    get() = when (this) {
        is ClassName -> false
        Dynamic -> false
        is LambdaTypeName -> receiver?.hasTypeVariable ?: false || parameters.any { it.type.hasTypeVariable } || returnType.hasTypeVariable
        is ParameterizedTypeName -> typeArguments.any { it.hasTypeVariable }
        is TypeVariableName -> true
        is WildcardTypeName -> inTypes.any { it.hasTypeVariable } || outTypes.any { it.hasTypeVariable }
    }

internal fun CodeBlock.Builder.add(resolver: Resolver, argument: KSValueArgument, type: KSTypeReference) {
    val name = argument.name!!.getShortName()
    add("%S·to·", name)
    when (val value = argument.value!!) {
        is KSType -> add("%T::class.java", value.starProjection().toTypeName())
        is KSName -> add("%T.%L", ClassName.bestGuess(value.getQualifier()), value.getShortName())
        is KSAnnotation -> add("%L", add(resolver, value))
        else -> add(memberForValue(resolver, value, type.resolve()))
    }
}

/**
 * Creates a [CodeBlock] with parameter `format` depending on the given `value` object.
 * Handles a number of special cases, such as appending "f" to `Float` values, and uses
 * `%L` for other types.
 */
internal fun memberForValue(resolver: Resolver, value: Any?, type: KSType): CodeBlock = when (value) {
    is Class<*> -> CodeBlock.of("%T::class.java", value)
    is Enum<*> -> CodeBlock.of("%T.%L", value.javaClass, value.name)
    is String -> CodeBlock.of("%S", value)
    is Float -> CodeBlock.of("%Lf", value)
    is Char -> CodeBlock.of("'%L'", if (value == '\'') "\\'" else value)
    is Boolean -> CodeBlock.of("%L", value)
    is Double -> {
        var s = value.toString()
        if ('.' !in s) s += ".0"
        CodeBlock.of("%L", s)
    }
    is Byte -> CodeBlock.of("(%L).toByte()", value)
    is Short -> CodeBlock.of("(%L).toShort()", value)
    is Int -> CodeBlock.of("%L", value)
    is Long -> CodeBlock.of("%LL", value)
    null -> CodeBlock.of("null")
    is List<*> -> {
        CodeBlock.builder()
            .add(
                "%L(", when (val typeName = type.toTypeName()) {
                    BYTE, BYTE_ARRAY -> "byteArrayOf"
                    CHAR, CHAR_ARRAY -> "charArrayOf"
                    SHORT, SHORT_ARRAY -> "shortArrayOf"
                    INT, INT_ARRAY -> "intArrayOf"
                    LONG, LONG_ARRAY -> "longArrayOf"
                    FLOAT, FLOAT_ARRAY -> "floatArrayOf"
                    DOUBLE, DOUBLE_ARRAY -> "doubleArrayOf"
                    BOOLEAN, BOOLEAN_ARRAY -> "booleanArrayOf"
                    else -> {
                        CodeBlock.of(
                            "arrayOf<%T>", if (typeName is ParameterizedTypeName) {
                                typeName.typeArguments.single()
                            } else {
                                typeName
                            }
                        )
                    }
                }
            )
            .applyEachIndexed(value) { i, item ->
                if (i > 0) {
                    add(", ")
                }
                add("%L", memberForValue(resolver, item, type))
            }
            .add(")")
            .build()
    }
    else -> throw Exception("Unknown type ${value.javaClass}")
}