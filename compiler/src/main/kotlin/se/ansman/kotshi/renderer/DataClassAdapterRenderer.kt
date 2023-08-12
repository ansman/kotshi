package se.ansman.kotshi.renderer

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.plusParameter
import se.ansman.kotshi.*
import se.ansman.kotshi.Types.Moshi.jsonDataException
import se.ansman.kotshi.Types.Moshi.jsonReaderToken
import se.ansman.kotshi.model.*
import java.lang.reflect.Constructor
import org.objectweb.asm.Type as AsmType

class DataClassAdapterRenderer(
    private val adapter: DataClassJsonAdapter,
    private val createAnnotationsUsingConstructor: Boolean
) : AdapterRenderer(adapter) {
    private val adapterKeys = adapter.properties.generatePropertySpecs()
    private val propertyNames = adapter.properties.mapTo(mutableSetOf()) { it.jsonName }
    private val parentLabels = adapter.polymorphicLabels.filterKeys { it !in propertyNames }
    private val serializedNames = adapter.serializedProperties.map { it.jsonName }.toSet()
    private val optionsProperty = jsonOptionsProperty(serializedNames + parentLabels.keys)
    private val hasDefaultValueConstructor = adapter.serializedProperties.any { it.hasDefaultValue }

    private val constructorProperty = PropertySpec
        .builder(
            nameAllocator.newName("defaultConstructor"),
            Constructor::class.asClassName().parameterizedBy(adapter.targetType).nullable(),
            KModifier.PRIVATE
        )
        .addAnnotation(Volatile::class)
        .mutable(true)
        .initializer("null")
        .build()

    override fun TypeSpec.createProguardRule(): ProguardConfig? {
        if (!hasDefaultValueConstructor) {
            return null
        }
        var parameterTypes = emptyList<String>()
        adapter.constructorSignature.let { constructorSignature ->
            if (constructorSignature.startsWith("constructor-impl")) {
                // Inline class, we don't support this yet.
                // This is a static method with signature like 'constructor-impl(I)I'
                return@let
            }
            parameterTypes = AsmType.getArgumentTypes(constructorSignature.removePrefix("<init>"))
                .map { it.toReflectionString() }
        }
        return ProguardConfig(
            targetClass = adapter.adapterClassName,
            targetConstructorParams = parameterTypes,
        )
    }

    override fun TypeSpec.Builder.renderSetup() {
        primaryConstructor(
            FunSpec.constructorBuilder()
                .applyIf(adapterKeys.isNotEmpty()) { addParameter(moshiParameterName, Types.Moshi.moshi) }
                .applyIf(adapterKeys.any { it.key.type.hasTypeVariable }) {
                    addParameter(typesParameterName, Types.Kotshi.typesArray)
                }
                .build()
        )
        addProperties(adapterKeys.values)
        addProperty(optionsProperty)

        if (hasDefaultValueConstructor) {
            addProperty(constructorProperty)
        }
    }

    private fun Iterable<Property>.generatePropertySpecs(): Map<Property, PropertySpec> =
        asSequence()
            .filter { it.shouldUseAdapter }
            .associateWith { property ->
                PropertySpec
                    .builder(
                        nameAllocator.newName(property.suggestedAdapterName),
                        Types.Moshi.jsonAdapter.plusParameter(property.type),
                        KModifier.PRIVATE
                    )
                    .initializer(
                        CodeBlock.builder()
                            .add("moshi.adapter(«\n")
                            .add(property.asRuntimeType { typeVariableName ->
                                val genericIndex =
                                    adapter.targetTypeVariables.indexOfFirst { it.name == typeVariableName.name }
                                require(genericIndex >= 0) {
                                    throw IllegalStateException("Element ${adapter.targetType} is generic but is of an unknown type")
                                }
                                CodeBlock.of("types[$genericIndex]")
                            })
                            .add(",\n%L", property.annotations(createAnnotationsUsingConstructor))
                            .add(",\n%S", property.name)
                            .add("\n»)")
                            .build()
                    )
                    .build()
            }

    override fun FunSpec.Builder.renderToJson(
        writerParameter: ParameterSpec,
        valueParameter: ParameterSpec
    ) {
        fun addBody(): FunSpec.Builder =
            addStatement("%N", writerParameter)
                .addCode("⇥")
                .addStatement(".beginObject()")
                .applyEach(parentLabels.entries) { (key, value) ->
                    addStatement(".name(%S).value(%S)", key, value)
                }
                .applyEach(adapter.serializedProperties) { property ->
                    addCode(".name(%S)", property.jsonName)
                    val getter = CodeBlock.of("%N.%N", valueParameter, property.name)

                    if (property.shouldUseAdapter) {
                        addCode(".%M {\n", Functions.Kotlin.apply)
                        addCode("⇥")
                        addCode("%N.toJson(this, ", adapterKeys.getValue(property))
                        addCode(getter)
                        addCode(")\n")
                        addCode("⇤")
                        addCode("}")
                    } else when (property.type.notNull()) {
                        STRING,
                        INT,
                        LONG,
                        FLOAT,
                        DOUBLE,
                        SHORT,
                        BOOLEAN -> addCode(".value(%L)", getter)
                        BYTE -> addCode(".%M(%L)", Functions.Kotshi.byteValue, getter)
                        CHAR -> addCode(".%M(%L)", Functions.Kotshi.value, getter)
                        else -> error("Property ${property.name} is not primitive ${property.type} but requested non adapter use")
                    }
                    addCode("\n")
                }
                .addStatement(".endObject()", writerParameter)
                .addCode("⇤")

        addIf("%N == null", valueParameter) {
            addStatement("%N.nullValue()", writerParameter)
            addStatement("return")
        }

        val serializeNullsEnabled = when (adapter.serializeNulls) {
            SerializeNulls.DEFAULT -> null
            SerializeNulls.ENABLED -> true
            SerializeNulls.DISABLED -> false
        }
        if (serializeNullsEnabled != null) {
            this
                .addStatement("val·serializeNulls·= %N.serializeNulls", writerParameter)
                .addStatement("%N.serializeNulls·= %L", writerParameter, serializeNullsEnabled)
                .beginControlFlow("try")
                .apply { addBody() }
                .nextControlFlow("finally")
                .addStatement("%N.serializeNulls·= serializeNulls", writerParameter)
                .endControlFlow()
        } else {
            addBody()
        }
    }

    override fun FunSpec.Builder.renderFromJson(readerParameter: ParameterSpec) {
        val maskCount = if (hasDefaultValueConstructor) {
            (adapter.properties.size + 31) / 32
        } else {
            0
        }
        val maskNames = Array(maskCount) { index ->
            nameAllocator.newName(if (maskCount == 1) "mask" else "mask${index + 1}")
        }
        val maskAllSetValues = Array(maskCount) { -1 }
        var maskIndex = 0
        var maskNameIndex = 0
        val variables = LinkedHashMap<Property, PropertyVariables>(adapter.properties.size)
        for (property in adapter.properties) {
            if (!property.isIgnored) {
                val inverted = (1 shl maskIndex).inv()
                if (property.hasDefaultValue) {
                    maskAllSetValues[maskNameIndex] = maskAllSetValues[maskNameIndex] and inverted
                }
                variables[property] = property.createVariables(
                    maskName = if (hasDefaultValueConstructor) maskNames[maskNameIndex] else null,
                    maskIndex = maskIndex
                )
            }
            ++maskIndex
            if (maskIndex == 32) {
                // Move to the next mask
                maskIndex = 0
                ++maskNameIndex
            }
        }

        this
            .addStatement(
                "if·(%N.peek()·==·%T.NULL)·return·%N.nextNull()",
                readerParameter,
                jsonReaderToken,
                readerParameter
            )
            .addCode("\n")
            .applyIf(hasDefaultValueConstructor) {
                // Initialize all our masks, defaulting to fully unset (-1)
                for (maskName in maskNames) {
                    addStatement("var·%L·=·-1", maskName)
                }
            }
            .applyEach(variables.values) { variable ->
                addCode("%L", variable.value)
                if (variable.localIsSet != null) {
                    addCode("%L", variable.localIsSet)
                }
            }
            .addCode("\n")
            .addStatement("%N.beginObject()", readerParameter)
            .addWhile("%N.hasNext()", readerParameter) {
                addWhen("%N.selectName(%N)", readerParameter, optionsProperty) {
                    adapter.serializedProperties.forEachIndexed { propertyIndex, property ->
                        val variable = variables.getValue(property)
                        addWhenBranch("%L", propertyIndex) {
                            if (property.shouldUseAdapter) {
                                addStatement(
                                    "%N·= %N.fromJson(%N)",
                                    variable.value,
                                    adapterKeys.getValue(property),
                                    readerParameter
                                )
                                if (variable.markSet != null) {
                                    if (property.type.isNullable) {
                                        addCode(variable.markSet)
                                    } else {
                                        addCode("⇥")
                                        addControlFlow("?.also") {
                                            addCode(variable.markSet)
                                        }
                                        addCode("⇤")
                                    }
                                }
                            } else {
                                fun FunSpec.Builder.readPrimitive(functionName: String, vararg args: Any) {
                                    addIfElse(
                                        "%N.peek() == %T.NULL",
                                        readerParameter, jsonReaderToken
                                    ) {
                                        addStatement("%N.skipValue()", readerParameter)
                                    }
                                    addElse {
                                        addStatement(
                                            "%N·= %N.$functionName()",
                                            variable.value,
                                            readerParameter,
                                            *args
                                        )
                                        if (variable.markSet != null && !property.type.isNullable) {
                                            addCode(variable.markSet)
                                        }
                                    }
                                    if (variable.markSet != null && property.type.isNullable) {
                                        addCode(variable.markSet)
                                    }
                                }

                                when (property.type.notNull()) {
                                    STRING -> readPrimitive("nextString")
                                    BOOLEAN -> readPrimitive("nextBoolean")
                                    BYTE -> readPrimitive("%M", Functions.Kotshi.nextByte)
                                    SHORT -> readPrimitive("%M", Functions.Kotshi.nextShort)
                                    INT -> readPrimitive("nextInt")
                                    LONG -> readPrimitive("nextLong")
                                    CHAR -> readPrimitive("%M", Functions.Kotshi.nextChar)
                                    FLOAT -> readPrimitive("%M", Functions.Kotshi.nextFloat)
                                    DOUBLE -> readPrimitive("nextDouble")
                                    else -> error(
                                        "Internal Kotshi error when processing ${adapter.targetType}. " +
                                            "Expected property type to be a primitive but was ${property.type}. " +
                                            "Please open an issue here: https://github.com/ansman/kotshi/issues/new"
                                    )
                                }
                            }
                        }
                    }
                    parentLabels.keys.forEachIndexed { i, name ->
                        addWhenBranch("%L", variables.size + i) {
                            addComment("Consumes the '%L' label key from the parent", name)
                            addStatement("%N.nextString()", readerParameter)
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
                                    Functions.Kotshi.appendNullableError,
                                    property.name
                                )
                            } else {
                                addStatement(
                                    "%N = %N.%M(%S, %S)",
                                    stringBuilder,
                                    stringBuilder,
                                    Functions.Kotshi.appendNullableError,
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
                addCode("return·")
                if (hasDefaultValueConstructor) {
                    val allMasksAreSetBlock = maskNames.withIndex()
                        .map { (index, maskName) ->
                            CodeBlock.of("$maskName·== 0x${Integer.toHexString(maskAllSetValues[index])}.toInt()")
                        }
                        .joinToCode("·&& ")
                    beginControlFlow("if (%L)", allMasksAreSetBlock)
                }

                addCode("%T(«", adapter.targetType)
                variables.entries.forEachIndexed { index, (property, variable) ->
                    if (index > 0) {
                        addCode(",")
                    }
                    addCode("\n%N·= %N", property.name, variable.value.name)
                    if (variable.value.type.isNullable && !property.type.isNullable) {
                        addCode("!!")
                    }
                }
                addCode("»\n)\n")
                if (hasDefaultValueConstructor) {
                    nextControlFlow("else")
                    addComment("Reflectively invoke the synthetic defaults constructor")
                    // Dynamic default constructor call
                    val nonNullConstructorType = constructorProperty.type.copy(nullable = false)
                    val constructorPropertyTypes = adapter.properties.map {
                        it.type.unwrapTypeAlias().asTypeBlock()
                    }
                    val args = constructorPropertyTypes
                        .plus(0.until(maskCount).map { INT_TYPE_BLOCK }) // Masks, one every 32 params
                        .plus(DEFAULT_CONSTRUCTOR_MARKER_TYPE_BLOCK) // Default constructor marker is always last
                        .joinToCode(",\n")
                    val coreLookupBlock = CodeBlock
                        .builder()
                        .add("%T::class.java.getDeclaredConstructor(\n", adapter.rawTargetType)
                        .indent()
                        .add(args)
                        .unindent()
                        .add("\n)")
                        .build()
                    val lookupBlock = if (adapter.targetTypeVariables.isNotEmpty()) {
                        CodeBlock.of("(%L·as·%T)", coreLookupBlock, nonNullConstructorType)
                    } else {
                        coreLookupBlock
                    }
                    val initializerBlock = CodeBlock.of(
                        "this.%1N·?: %2L.also·{ this.%1N·= it }",
                        constructorProperty,
                        lookupBlock
                    )
                    val localConstructorProperty = PropertySpec
                        .builder(
                            nameAllocator.newName("localConstructor"),
                            nonNullConstructorType
                        )
                        .addAnnotation(
                            AnnotationSpec.builder(Suppress::class)
                                .addMember("%S", "UNCHECKED_CAST")
                                .build()
                        )
                        .initializer(initializerBlock)
                        .build()
                    addCode("%L", localConstructorProperty)
                    addCode("«%N.newInstance(", localConstructorProperty)
                    var separator = "\n"
                    for (property in adapter.properties) {
                        addCode(separator)
                        if (property.isIgnored) {
                            // We have to use the default primitive for the available type in order for
                            // invokeDefaultConstructor to properly invoke it. Just using "null" isn't safe because
                            // the transient type may be a primitive type.
                            // Inline a little comment for readability indicating which parameter is it's referring to
                            addCode(
                                "/*·%L·*/·%L",
                                property.name,
                                property.type.rawType().defaultPrimitiveValue()
                            )
                        } else {
                            val variable = variables.getValue(property)
                            addCode("%N", variable.value)
                            if (variable.value.type.isNullable && property.type.isPrimitive) {
                                addCode(" ?: %L", property.type.defaultPrimitiveValue())
                            }
                        }
                        separator = ",\n"
                    }
                    for (maskName in maskNames) {
                        addCode(",\n%L", maskName)
                    }
                    addCode(",\n/*·DefaultConstructorMarker·*/·null\n»)\n")
                    endControlFlow()
                }
            }
    }

    private fun Property.createVariables(
        maskName: String?,
        maskIndex: Int,
    ): PropertyVariables {
        val valueType = if (type.isPrimitive && !shouldUseAdapter) type else type.nullable()
        val localIsSet = if (valueType.isPrimitive && !hasDefaultValue) {
            PropertySpec
                .builder(nameAllocator.newName("${variableName}IsSet"), BOOLEAN)
                .mutable()
                .initializer("false")
                .build()
        } else {
            null
        }
        val mask = 1 shl maskIndex
        val value = PropertySpec
            .builder(
                name = nameAllocator.newName(variableName),
                type = valueType
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
            .build()
        return PropertyVariables(
            value = value,
            localIsSet = localIsSet,
            markSet = if (localIsSet != null) {
                CodeBlock.builder().addStatement("%N·=·true", localIsSet).build()
            } else if (hasDefaultValue) {
                CodeBlock.builder()
                    .add("// \$mask = \$mask and (1 shl %L).inv()\n", maskIndex)
                    .addStatement("%1L = %1L and 0x%2L.toInt()", maskName, Integer.toHexString(mask.inv()))
                    .build()
            } else {
                null
            },
            isSet = if (localIsSet != null) {
                CodeBlock.of("%N", localIsSet)
            } else if (hasDefaultValue) {
                CodeBlock.of("%1L and 0x%2L.toInt() != 0", maskName, Integer.toHexString(mask))
            } else {
                CodeBlock.of("%N != null", value)
            },
            isNotSet = if (localIsSet != null) {
                CodeBlock.of("!%N", localIsSet)
            } else if (hasDefaultValue) {
                CodeBlock.of("%1L and 0x%2L.toInt() == 0", maskName, Integer.toHexString(mask))
            } else {
                CodeBlock.of("%N == null", value)
            },
        )
    }

}

private val INT_TYPE_BLOCK = CodeBlock.of("%T::class.javaPrimitiveType", INT)
private val DEFAULT_CONSTRUCTOR_MARKER_TYPE_BLOCK = CodeBlock.of(
    "%T::class.java",
    ClassName("kotlin.jvm.internal", "DefaultConstructorMarker")
)

private data class PropertyVariables(
    val value: PropertySpec,
    val localIsSet: PropertySpec?,
    val markSet: CodeBlock?,
    val isNotSet: CodeBlock,
    val isSet: CodeBlock,
)

private fun Property.annotations(createAnnotationsUsingConstructor: Boolean): CodeBlock =
    CodeBlock.builder()
        .add("%M(", Functions.Kotlin.setOf)
        .applyIf(jsonQualifiers.size > 1) {
            indent()
            add("\n")
        }
        .applyEachIndexed(jsonQualifiers.sortedBy { it.annotationName }) { index, qualifier ->
            if (index > 0) add(",\n")
            add(qualifier.render(createAnnotationsUsingConstructor))
        }
        .applyIf(jsonQualifiers.size > 1) {
            unindent()
            add("\n")
        }
        .add(")")
        .build()


private val TypeName.hasTypeVariable: Boolean
    get() = when (this) {
        is ClassName -> false
        Dynamic -> false
        is LambdaTypeName -> receiver?.hasTypeVariable ?: false || parameters.any { it.type.hasTypeVariable } || returnType.hasTypeVariable
        is ParameterizedTypeName -> typeArguments.any { it.hasTypeVariable }
        is TypeVariableName -> true
        is WildcardTypeName -> inTypes.any { it.hasTypeVariable } || outTypes.any { it.hasTypeVariable }
    }


@OptIn(DelicateKotlinPoetApi::class)
private fun TypeName.asTypeBlock(): CodeBlock {
    if (annotations.isNotEmpty()) {
        return copy(annotations = emptyList()).asTypeBlock()
    }
    return when (this) {
        is ParameterizedTypeName -> {
            if (rawType == ARRAY) {
                val componentType = typeArguments[0]
                if (componentType is ParameterizedTypeName) {
                    // "generic" array just uses the component's raw type
                    // java.lang.reflect.Array.newInstance(<raw-type>, 0).javaClass
                    CodeBlock.of(
                        "%T.newInstance(%L, 0).javaClass",
                        Array::class.java.asClassName(),
                        componentType.rawType.asTypeBlock()
                    )
                } else {
                    CodeBlock.of("%T::class.java", copy(nullable = false))
                }
            } else {
                rawType.asTypeBlock()
            }
        }

        is TypeVariableName -> (bounds.firstOrNull() ?: ANY).asTypeBlock()
        is LambdaTypeName -> rawType().asTypeBlock()
        is ClassName -> {
            // Check against the non-nullable version for equality, but we'll keep the nullability in
            // consideration when creating the CodeBlock if needed.
            when (val className = notNull()) {
                BOOLEAN,
                CHAR,
                BYTE,
                SHORT,
                INT,
                FLOAT,
                LONG,
                DOUBLE ->
                    if (isNullable) {
                        // Remove nullable but keep the java object type
                        CodeBlock.of("%T::class.javaObjectType", className)
                    } else {
                        CodeBlock.of("%T::class.javaPrimitiveType", this)
                    }

                Types.Java.void,
                NOTHING -> error("Parameter with void, or Nothing type is illegal")

                else -> CodeBlock.of("%T::class.java", className)
            }
        }

        else -> throw UnsupportedOperationException("Parameter with type '${javaClass.simpleName}' is illegal. Only classes, parameterized types, or type variables are allowed.")
    }
}

private fun TypeName.defaultPrimitiveValue(): CodeBlock =
    when (this) {
        BOOLEAN -> CodeBlock.of("false")
        CHAR -> CodeBlock.of("'\\u0000'")
        BYTE -> CodeBlock.of("0.toByte()")
        SHORT -> CodeBlock.of("0.toShort()")
        INT -> CodeBlock.of("0")
        FLOAT -> CodeBlock.of("0f")
        LONG -> CodeBlock.of("0L")
        DOUBLE -> CodeBlock.of("0.0")
        else -> CodeBlock.of("null")
    }

private fun AsmType.toReflectionString(): String =
    when (this) {
        AsmType.VOID_TYPE -> "void"
        AsmType.BOOLEAN_TYPE -> "boolean"
        AsmType.CHAR_TYPE -> "char"
        AsmType.BYTE_TYPE -> "byte"
        AsmType.SHORT_TYPE -> "short"
        AsmType.INT_TYPE -> "int"
        AsmType.FLOAT_TYPE -> "float"
        AsmType.LONG_TYPE -> "long"
        AsmType.DOUBLE_TYPE -> "double"
        else -> when (sort) {
            AsmType.ARRAY -> "${elementType.toReflectionString()}[]"
            // Object type
            else -> className
        }
    }