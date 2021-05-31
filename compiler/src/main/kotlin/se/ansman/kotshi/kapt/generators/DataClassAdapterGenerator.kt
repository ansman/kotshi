package se.ansman.kotshi.kapt.generators

import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.BYTE
import com.squareup.kotlinpoet.CHAR
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.Dynamic
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.plusParameter
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.jvm.throws
import com.squareup.kotlinpoet.metadata.ImmutableKmClass
import com.squareup.kotlinpoet.metadata.isData
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
import se.ansman.kotshi.kapt.MetadataAccessor
import se.ansman.kotshi.kapt.ProcessingError
import se.ansman.kotshi.notNull
import se.ansman.kotshi.nullable
import se.ansman.kotshi.suggestedAdapterName
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.AnnotationValueVisitor
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

class DataClassAdapterGenerator(
    metadataAccessor: MetadataAccessor,
    types: Types,
    elements: Elements,
    element: TypeElement,
    metadata: ImmutableKmClass,
    globalConfig: GlobalConfig
) : AdapterGenerator(metadataAccessor, types, elements, element, metadata, globalConfig) {
    init {
        require(metadata.isData)
    }

    private val serializeNulls = element.getAnnotation(JsonSerializable::class.java)!!
        .serializeNulls
        .takeUnless { it == SerializeNulls.DEFAULT }
        ?: globalConfig.serializeNulls

    override fun TypeSpec.Builder.addMethods() {
        val properties = elementTypeSpec.primaryConstructor!!.parameters.map { parameter ->
            Property.create(
                elements = elements,
                typeSpec = elementTypeSpec,
                globalConfig = globalConfig,
                enclosingClass = element,
                parameter = parameter
            )
        }
        if (properties.isEmpty()) {
            throw ProcessingError("Could not find any data class properties.", element)
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
                    val annotation = requireNotNull(qualifier.tag<AnnotationMirror>())
                    add(annotation)
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
                .initializer(CodeBlock.builder()
                    .add("«%N.adapter(", moshiParameter)
                    .add(adapterKey.asRuntimeType { typeVariableName ->
                        val genericIndex = elementTypeSpec.typeVariables.indexOfFirst { it.name == typeVariableName.name }
                        if (genericIndex == -1) {
                            throw ProcessingError("Element is generic but is of an unknown type", element)
                        } else {
                            CodeBlock.of("%N[$genericIndex]", typesParameter)
                        }
                    })
                    .add(adapterKey.annotations())
                    .add(")»")
                    .build())
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
                        addCode("%N.toJson(%N, ", adapterKeys.getValue(property.adapterKey), writerParameter).addCode(getter).addCode(")\n")
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
                        else -> throw ProcessingError("Property ${property.name} is not primitive ${property.type} but requested non adapter use", element)
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
            .addStatement("if (%N.peek() == %T.NULL) return %N.nextNull()", readerParameter, jsonReaderToken, readerParameter)
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
                                addStatement("%N·= %N.fromJson(%N)", variable.value, adapters.getValue(property.adapterKey), readerParameter)
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
                                        addStatement("%N·= %N.$functionName()", variable.value, readerParameter, *args)
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
                                    else -> throw ProcessingError("Internal Kotshi error. Expected property type to be a primitive but was ${property.type}. Please open an issue here: https://github.com/ansman/kotshi/issues/new", element)
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
                                addStatement("%N = %N.%M(%S)", stringBuilder, stringBuilder, kotshiUtilsAppendNullableError, property.name)
                            } else {
                                addStatement("%N = %N.%M(%S, %S)", stringBuilder, stringBuilder, kotshiUtilsAppendNullableError, property.name, property.jsonName)
                            }
                        }
                    }

                    addIf("%N != null", stringBuilder) {
                        addStatement("%N.append(\" (at path \").append(%N.path).append(')')", stringBuilder, readerParameter)
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
                .initializer(when {
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
                })
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
private fun CodeBlock.Builder.add(value: AnnotationValue, valueType: TypeMirror): CodeBlock.Builder = apply {
    value.accept(object : AnnotationValueVisitor<Unit, Nothing?> {
        override fun visitFloat(f: Float, p: Nothing?) {
            add("${f}f")
        }

        override fun visitByte(b: Byte, p: Nothing?) {
            add("(${b}).toByte()")
        }

        override fun visitShort(s: Short, p: Nothing?) {
            add("($s).toShort()")
        }

        override fun visitChar(c: Char, p: Nothing?) {
            if (c == '\'') {
                add("'\\''")
            } else {
                add("'$c'")
            }
        }

        override fun visitUnknown(av: AnnotationValue?, p: Nothing?) = throw AssertionError()

        override fun visit(av: AnnotationValue?, p: Nothing?) = throw AssertionError()

        override fun visit(av: AnnotationValue?) =throw AssertionError()

        override fun visitArray(vals: List<AnnotationValue>, p: Nothing?) {
            val arrayCreator = when ((valueType.asTypeName() as ParameterizedTypeName).typeArguments.single()) {
                BYTE -> "byteArrayOf"
                CHAR -> "charArrayOf"
                SHORT -> "shortArrayOf"
                INT -> "intArrayOf"
                LONG -> "longArrayOf"
                FLOAT -> "floatArrayOf"
                DOUBLE -> "doubleArrayOf"
                BOOLEAN -> "booleanArrayOf"
                else -> "arrayOf"
            }

            if (vals.isEmpty()) {
                add("$arrayCreator()")
            } else {
                add("$arrayCreator(")
                if (vals.size > 1) {
                    add("⇥\n")
                }
                vals.forEachIndexed { i, value ->
                    if (i > 0) {
                        add(",\n")
                    }
                    value.accept(this, null)
                }
                if (vals.size > 1) {
                    add("⇤\n")
                }
                add(")")
            }
        }

        override fun visitBoolean(b: Boolean, p: Nothing?) {
            add(if (b) "true" else "false")
        }

        override fun visitLong(i: Long, p: Nothing?) {
            add("${i}L")
        }

        override fun visitType(t: TypeMirror, p: Nothing?) {
            add("%T::class.java", t.asTypeName())
        }

        override fun visitString(s: String, p: Nothing?) {
            add("%S", s)
        }

        override fun visitDouble(d: Double, p: Nothing?) {
            val s = d.toString()
            add(s)
            if ('.' !in s) add(".0")
        }

        override fun visitEnumConstant(c: VariableElement, p: Nothing?) {
            add("%T.%N", c.asType().asTypeName(), c.simpleName)
        }

        override fun visitAnnotation(a: AnnotationMirror, p: Nothing?) {
            add(a)
        }

        override fun visitInt(i: Int, p: Nothing?) {
            add("$i")
        }
    }, null)
}

private fun CodeBlock.Builder.add(annotation: AnnotationMirror): CodeBlock.Builder = apply {
    if (annotation.elementValues.isEmpty()) {
        add("%T::class.java.%M()", annotation.annotationType.asTypeName(), kotshiUtilsCreateJsonQualifierImplementation)
    } else {
        add("%T::class.java.%M(mapOf(⇥", annotation.annotationType.asTypeName(), kotshiUtilsCreateJsonQualifierImplementation)
        annotation.elementValues.entries.forEachIndexed { i, (element, value) ->
            if (i > 0) {
                add(",")
            }
            add("\n")
            add("%S·to·", element.simpleName)
            add(value, element.returnType)
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