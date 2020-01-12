@file:Suppress("UnstableApiUsage")

package se.ansman.kotshi

import com.google.auto.common.MoreElements
import com.google.common.collect.SetMultimap
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.BYTE
import com.squareup.kotlinpoet.CHAR
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.NameAllocator
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.plusParameter
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.jvm.throws
import com.squareup.kotlinpoet.metadata.isData
import com.squareup.kotlinpoet.metadata.isEnum
import com.squareup.kotlinpoet.metadata.isInner
import com.squareup.kotlinpoet.metadata.isInternal
import com.squareup.kotlinpoet.metadata.isLocal
import com.squareup.kotlinpoet.metadata.isPublic
import com.squareup.kotlinpoet.metadata.specs.ClassInspector
import com.squareup.kotlinpoet.metadata.specs.toTypeSpec
import com.squareup.kotlinpoet.metadata.toImmutableKmClass
import com.squareup.kotlinpoet.tag
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import se.ansman.kotshi.SerializeNulls.DEFAULT
import se.ansman.kotshi.SerializeNulls.DISABLED
import se.ansman.kotshi.SerializeNulls.ENABLED
import java.io.IOException
import java.lang.reflect.Type
import javax.annotation.processing.Filer
import javax.annotation.processing.Messager
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.AnnotationValueVisitor
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements
import javax.tools.Diagnostic

class AdaptersProcessingStep(
    override val processor: KotshiProcessor,
    private val classInspector: ClassInspector,
    private val messager: Messager,
    override val filer: Filer,
    private val adapters: MutableList<GeneratedAdapter>,
    private val elements: Elements,
    private val sourceVersion: SourceVersion
) : KotshiProcessor.GeneratingProcessingStep() {
    override val annotations: Set<Class<out Annotation>> =
        setOf(JsonSerializable::class.java, KotshiJsonAdapterFactory::class.java)

    override fun process(elementsByAnnotation: SetMultimap<Class<out Annotation>, Element>, roundEnv: RoundEnvironment) {
        val globalConfig = (elementsByAnnotation[KotshiJsonAdapterFactory::class.java]
            .firstOrNull()
            ?.getAnnotation(KotshiJsonAdapterFactory::class.java)
            ?.let(::GlobalConfig)
            ?: GlobalConfig.DEFAULT)

        for (element in elementsByAnnotation[JsonSerializable::class.java]) {
            try {
                generateJsonAdapter(globalConfig, element)
            } catch (e: ProcessingError) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Kotshi: ${e.message}", e.element)
            }
        }
    }

    private fun generateJsonAdapter(globalConfig: GlobalConfig, element: Element) {
        val nameAllocator = NameAllocator()
        val imports = mutableSetOf<Import>()

        val enclosingClass = MoreElements.asType(element)
        val typeMirror = enclosingClass.asType()

        val metadata = element.metadata.toImmutableKmClass()

        when {
            !metadata.isData && !metadata.isEnum ->
                throw ProcessingError("@JsonSerializable can't be applied to $element: must be a Kotlin data class or enum", element)
            metadata.isInner ->
                throw ProcessingError("@JsonSerializable can't be applied to inner classes", element)
            metadata.isLocal ->
                throw ProcessingError("@JsonSerializable can't be applied to local classes", element)
            !metadata.isPublic && !metadata.isInternal ->
                throw ProcessingError("Classes annotated with @JsonSerializable must public or internal", element)
        }

        val elementTypeSpec = metadata.toTypeSpec(classInspector)
        val className = enclosingClass.asClassName()

        val typeVariables: List<TypeVariableName> = elementTypeSpec.typeVariables
            // Removes the variance
            .map { TypeVariableName(it.name, *it.bounds.toTypedArray()) }

        val typeName = if (typeVariables.isEmpty()) {
            className
        } else {
            className.parameterizedBy(typeVariables)
        }

        val adapterClassName = ClassName(className.packageName, "Kotshi${className.simpleNames.joinToString("_")}JsonAdapter")

        nameAllocator.newName("options")
        nameAllocator.newName("value")
        nameAllocator.newName("writer")
        nameAllocator.newName("reader")
        nameAllocator.newName("stringBuilder")
        nameAllocator.newName("serializeNulls")
        nameAllocator.newName("it")

        val moshiParameter = ParameterSpec.builder("moshi", Moshi::class.java)
            .build()

        val typesParameter = ParameterSpec.builder("types", Array<Type>::class.plusParameter(Type::class))
            .build()

        val jsonNames: Collection<String>

        val typeSpecBuilder = TypeSpec.classBuilder(adapterClassName)
            .addModifiers(KModifier.INTERNAL)
            .addOriginatingElement(element)
            .maybeAddGeneratedAnnotation(elements, sourceVersion)
            .addTypeVariables(typeVariables)
            .superclass(NamedJsonAdapter::class.asClassName().plusParameter(typeName))
            .addSuperclassConstructorParameter("%S", "KotshiJsonAdapter(${className.simpleNames.joinToString(".")})")

        if (metadata.isEnum) {
            jsonNames = typeSpecBuilder.addMethodsForEnum(elementTypeSpec, enclosingClass, typeName, className)
        } else {
            jsonNames = typeSpecBuilder.addMethodsForDataClass(
                elementTypeSpec = elementTypeSpec,
                globalConfig = globalConfig,
                enclosingClass = enclosingClass,
                moshiParameter = moshiParameter,
                typesParameter = typesParameter,
                nameAllocator = nameAllocator,
                typeVariables = typeVariables,
                imports = imports,
                typeName = typeName,
                typeMirror = typeMirror
            )
        }

        val options = PropertySpec.builder("options", JsonReader.Options::class, KModifier.PRIVATE)
            .addAnnotation(JvmStatic::class)
            .initializer(CodeBlock.Builder()
                .add("«%T.of(", jsonReaderOptions)
                .applyIf(jsonNames.size > 1) { add("\n") }
                .applyEachIndexed(jsonNames) { index, name ->
                    if (index > 0) {
                        add(",\n")
                    }
                    add("%S", name)
                }
                .applyIf(jsonNames.size > 1) { add("\n") }
                .add(")»")
                .build())
            .build()

        val typeSpec = typeSpecBuilder
            .applyIf(jsonNames.isNotEmpty()) {
                addType(TypeSpec.companionObjectBuilder()
                    .addModifiers(KModifier.PRIVATE)
                    .addProperty(options)
                    .build())
            }
            .build()

        adapters += GeneratedAdapter(
            targetType = className,
            className = adapterClassName,
            typeVariables = typeVariables,
            requiresMoshi = typeSpec.primaryConstructor
                ?.parameters
                ?.contains(moshiParameter)
                ?: false
        )

        FileSpec.builder(adapterClassName.packageName, adapterClassName.simpleName)
            .addComment("Code generated by Kotshi. Do not edit.")
            .addImports(imports)
            .addType(typeSpec)
            .build()
            .writeTo(filer)
    }

    private fun TypeSpec.Builder.addMethodsForEnum(
        elementTypeSpec: TypeSpec,
        enclosingClass: TypeElement,
        typeName: TypeName,
        className: ClassName
    ): Collection<String> {
        val enumToJsonName = elementTypeSpec.enumConstants.mapValues { (name, type) ->
            type.annotationSpecs.jsonName() ?: name
        }

        var defaultValue: String? = null
        for ((entry, spec) in elementTypeSpec.enumConstants) {
            if (spec.annotationSpecs.any { it.className == jsonDefaultValue }) {
                if (defaultValue != null) {
                    throw ProcessingError("Only one enum entry can be annotated with @JsonDefaultValue", enclosingClass)
                }
                defaultValue = entry
            }
        }

        val writer = ParameterSpec.builder("writer", JsonWriter::class.java)
            .build()
        val value = ParameterSpec.builder("value", typeName.nullable())
            .build()
        val reader = ParameterSpec.builder("reader", JsonReader::class.java)
            .build()

        addFunction(FunSpec.builder("toJson")
            .addModifiers(KModifier.OVERRIDE)
            .throws(IOException::class.java)
            .addParameter(writer)
            .addParameter(value)
            .addWhen("%N", value) {
                for ((entry, name) in enumToJsonName) {
                    addStatement("%T.%N·-> %N.value(%S)", className, entry, writer, name)
                }
                addStatement("null·-> %N.nullValue()", writer)
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
                .addNextControlFlow("else when (%N.selectString(options))", reader) {
                    enumToJsonName.keys.forEachIndexed { index, entry ->
                        addStatement("$index·-> %T.%N", className, entry)
                    }
                    if (defaultValue == null) {
                        addStatement("else·-> throw·%T(%P)", jsonDataException, "Expected one of ${enumToJsonName.values} but was \${${reader.name}.nextString()} at path \${${reader.name}.path}")
                    } else {
                        addControlFlow("else·->") {
                            addStatement("%N.skipValue()", reader)
                            addStatement("%T.%N", className, defaultValue)
                        }
                    }
                }
                .build())
        return enumToJsonName.values
    }

    private fun TypeSpec.Builder.addMethodsForDataClass(
        elementTypeSpec: TypeSpec,
        globalConfig: GlobalConfig,
        enclosingClass: TypeElement,
        moshiParameter: ParameterSpec,
        typesParameter: ParameterSpec,
        nameAllocator: NameAllocator,
        typeVariables: List<TypeVariableName>,
        imports: MutableSet<Import>,
        typeName: TypeName,
        typeMirror: TypeMirror
    ): Collection<String> {
        val annoation = requireNotNull(enclosingClass.getAnnotation(JsonSerializable::class.java))

        val properties = elementTypeSpec.primaryConstructor!!.parameters.map { parameter ->
            Property.create(
                elements = elements,
                typeSpec = elementTypeSpec,
                globalConfig = globalConfig,
                enclosingClass = enclosingClass,
                parameter = parameter
            )
        }

        val serializeNulls = annoation
            .serializeNulls
            .takeUnless { it == DEFAULT }
            ?: globalConfig.serializeNulls

        val adapterKeys = properties
            .asSequence()
            .filter { it.shouldUseAdapter }
            .map { it.adapterKey }
            .generatePropertySpecs(enclosingClass, moshiParameter, typesParameter, nameAllocator, typeVariables, imports)

        primaryConstructor(FunSpec.constructorBuilder()
            .applyIf(adapterKeys.isNotEmpty()) {
                addParameter(moshiParameter)
            }
            .applyIf(typeVariables.isNotEmpty()) {
                addParameter(typesParameter)
            }
            .build())
            .addProperties(adapterKeys.values)
            .addToJson(enclosingClass, typeName, properties, adapterKeys, imports, serializeNulls)
            .addFromJson(typeName, nameAllocator, typeMirror, properties, adapterKeys, imports)
            .build()

        return properties
            .asSequence()
            .filterNot { it.isTransient }
            .map { it.jsonName }
            .toList()
    }

    private fun Sequence<AdapterKey>.generatePropertySpecs(
        enclosingClass: TypeElement,
        moshiParameter: ParameterSpec,
        typesParameter: ParameterSpec,
        nameAllocator: NameAllocator,
        typeVariables: List<TypeVariableName>,
        imports: MutableSet<Import>
    ): Map<AdapterKey, PropertySpec> {
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
                    add(annotation, imports)
                }
                .applyIf(jsonQualifiers.size > 1) { add("⇤\n") }
                .add(")")
                .build()
        }

        return associateWith { adapterKey ->
            PropertySpec
                .builder(
                    nameAllocator.newName(adapterKey.suggestedAdapterName),
                    JsonAdapter::class.java.asClassName().plusParameter(adapterKey.type),
                    KModifier.PRIVATE
                )
                .initializer(CodeBlock.builder()
                    .add("«%N.adapter(", moshiParameter)
                    .add(adapterKey.asRuntimeType { typeVariableName ->
                        val genericIndex = typeVariables.indexOf(typeVariableName.notNull())
                        if (genericIndex == -1) {
                            throw ProcessingError("Element is generic but if an unknown type", enclosingClass)
                        } else {
                            CodeBlock.of("%N[$genericIndex]", typesParameter)
                        }
                    })
                    .add(adapterKey.annotations())
                    .add(")\n»")
                    .build())
                .build()
        }
    }

    private fun TypeSpec.Builder.addToJson(
        element: Element,
        typeName: TypeName,
        properties: Collection<Property>,
        adapterKeys: Map<AdapterKey, PropertySpec>,
        imports: MutableCollection<Import>,
        serializeNulls: SerializeNulls
    ): TypeSpec.Builder {
        val writer = ParameterSpec.builder("writer", JsonWriter::class.java)
            .build()
        val value = ParameterSpec.builder("value", typeName.nullable())
            .build()
        val builder = FunSpec.builder("toJson")
            .addModifiers(KModifier.OVERRIDE)
            .throws(IOException::class.java)
            .addParameter(writer)
            .addParameter(value)
        val serializeNullsEnabled = when (serializeNulls) {
            DEFAULT -> null
            ENABLED -> true
            DISABLED -> false
        }

        if (properties.isEmpty()) {
            builder
                .addIfElse("%N == null", value) {
                    addStatement("%N.nullValue()", writer)
                }
                .addElse {
                    addStatement("%N\n.beginObject()\n.endObject()", writer)
                }

        } else {
            fun FunSpec.Builder.addBody(): FunSpec.Builder =
                addStatement("%N.beginObject()", writer)
                    .addCode("\n")
                    .applyEach(properties.filterNot { it.isTransient }) { property ->
                        addStatement("%N.name(%S)", writer, property.jsonName)
                        val getter = CodeBlock.of("%N.%L", value, property.name)

                        if (property.shouldUseAdapter) {
                            addCode("%N.toJson(%N, ", adapterKeys.getValue(property.adapterKey), writer).addCode(getter).addCode(")\n")
                        } else when (property.type.notNull()) {
                            STRING,
                            INT,
                            LONG,
                            FLOAT,
                            DOUBLE,
                            SHORT,
                            BOOLEAN -> addStatement("%N.value(%L)", writer, getter)
                            BYTE -> {
                                imports += kotshiUtilsByteValue
                                addStatement("%N.byteValue(%L)", writer, getter)
                            }
                            CHAR -> {
                                imports += kotshiUtilsValue
                                addStatement("%N.value(%L)", writer, getter)
                            }
                            else -> throw ProcessingError("Property ${property.name} is not primitive ${property.type} but requested non adapter use", element)
                        }
                    }
                    .addCode("\n")
                    .addStatement("%N.endObject()", writer)

            builder
                .addIf("%N == null", value) {
                    addStatement("%N.nullValue()", writer)
                    addStatement("return")
                }

            if (serializeNullsEnabled != null) {
                builder
                    .addStatement("val serializeNulls = %N.serializeNulls", writer)
                    .addStatement("%N.serializeNulls = %L", writer, serializeNullsEnabled)
                    .beginControlFlow("try")
                    .addBody()
                    .nextControlFlow("finally")
                    .addStatement("%N.serializeNulls = serializeNulls", writer)
                    .endControlFlow()
            } else {
                builder.addBody()
            }
        }
        return addFunction(builder.build())
    }

    private fun TypeSpec.Builder.addFromJson(
        typeName: TypeName,
        nameAllocator: NameAllocator,
        type: TypeMirror,
        properties: Collection<Property>,
        adapters: Map<AdapterKey, PropertySpec>,
        imports: MutableCollection<Import>
    ): TypeSpec.Builder {
        val reader = ParameterSpec.builder("reader", JsonReader::class.java)
            .build()

        val builder = FunSpec.builder("fromJson")
            .addModifiers(KModifier.OVERRIDE)
            .throws(IOException::class.java)
            .addParameter(reader)
            .returns(typeName.nullable())

        if (properties.isEmpty()) {
            builder.addControlFlow("return when(%N.peek())", reader) {
                builder.addWhenBranch("%T.NULL", JsonReader.Token::class.java) {
                    addStatement("%N.skipValue()", reader)
                }
                builder.addWhenBranch("%T.BEGIN_OBJECT", JsonReader.Token::class.java) {
                    addStatement("%N.skipValue()", reader)
                    addStatement("%T()", type)
                }
                builder.addWhenElse {
                    addComment("Will throw an exception")
                    addStatement("%N.beginObject()", reader)
                    addStatement("throw %T()", AssertionError::class.java)
                }
            }
            return addFunction(builder.build())
        }


        val variables = properties
            .asSequence()
            .filterNot { it.isTransient }
            .associateBy({ it }, { it.createVariables(nameAllocator) })

        return addFunction(builder
            .addStatement("if (%N.peek() == %T.NULL) return %N.nextNull()", reader, JsonReader.Token::class.java, reader)
            .addCode("\n")
            .applyEach(variables.values) { variable ->
                addCode("%L", variable.value)
                if (variable.helper != null) {
                    addCode("%L", variable.helper)
                }
            }
            .addCode("\n")
            .addStatement("%N.beginObject()", reader)
            .addWhile("%N.hasNext()", reader) {
                addWhen("%N.selectName(options)", reader) {
                    variables.entries.forEachIndexed { index, (property, variable) ->
                        addWhenBranch("%L", index) {
                            if (property.shouldUseAdapter) {
                                addStatement("%N·= %N.fromJson(%N)", variable.value, adapters.getValue(property.adapterKey), reader)
                                if (variable.helper != null) {
                                    if (property.type.isNullable) {
                                        addStatement("%N·= true", variable.helper)
                                    } else {
                                        addStatement("?.also { %N = true }", variable.helper)
                                    }
                                }
                            } else {
                                fun FunSpec.Builder.readPrimitive(functionName: String) {
                                    addIfElse("%N.peek() == %T.NULL", reader, JsonReader.Token::class.java) {
                                        addStatement("%N.skipValue()", reader)
                                    }
                                    addElse {
                                        addStatement("%N·= %N.$functionName()", variable.value, reader)
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
                                    BYTE -> {
                                        imports += kotshiUtilsNextByte
                                        readPrimitive("nextByte")
                                    }
                                    SHORT -> {
                                        imports += kotshiUtilsNextShort
                                        readPrimitive("nextShort")
                                    }
                                    INT -> readPrimitive("nextInt")
                                    LONG -> readPrimitive("nextLong")
                                    CHAR -> {
                                        imports += kotshiUtilsNextChar
                                        readPrimitive("nextChar")
                                    }
                                    FLOAT -> {
                                        imports += kotshiUtilsNextFloat
                                        readPrimitive("nextFloat")
                                    }
                                    DOUBLE -> readPrimitive("nextDouble")
                                }
                            }
                        }
                    }
                    addWhenBranch("-1") {
                        addStatement("%N.skipName()", reader)
                        addStatement("%N.skipValue()", reader)
                    }
                }
            }
            .addStatement("%N.endObject()", reader)
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
                            imports += kotshiUtilsAppendNullableError
                            addStatement("%N = %N.appendNullableError(%S, %S)", stringBuilder, stringBuilder, property.name, property.jsonName)
                        }
                    }

                    addIf("%N != null", stringBuilder) {
                        addStatement("%N.append(\" (at path \").append(%N.path).append(')')", stringBuilder, reader)
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

                addCode("«return·%T(", type)
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
                addCode(")»")
                if (copyProperties.isNotEmpty()) {
                    addControlFlow(".let") {
                        addCode("«it.copy(")
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
                        addCode(")\n»")
                    }
                }
                addCode("\n")
            }
            .build())
    }
}

private fun Property.createVariables(nameAllocator: NameAllocator) =
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

@UseExperimental(ExperimentalStdlibApi::class)
fun CodeBlock.Builder.add(value: AnnotationValue, valueType: TypeMirror, imports: MutableCollection<Import>): CodeBlock.Builder = apply {
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
            add("'$c'")
        }

        override fun visitUnknown(av: AnnotationValue?, p: Nothing?) = throw AssertionError()

        override fun visit(av: AnnotationValue?, p: Nothing?) = throw AssertionError()

        override fun visit(av: AnnotationValue?) = throw AssertionError()

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
            add(a, imports)
        }

        override fun visitInt(i: Int, p: Nothing?) {
            add("$i")
        }
    }, null)
}

fun CodeBlock.Builder.add(annotation: AnnotationMirror, imports: MutableCollection<Import>): CodeBlock.Builder = apply {
    imports += kotshiUtilsCreateJsonQualifierImplementation
    if (annotation.elementValues.isEmpty()) {
        add("%T::class.java.createJsonQualifierImplementation()", annotation.annotationType.asTypeName())
    } else {
        add("%T::class.java.createJsonQualifierImplementation(mapOf(⇥", annotation.annotationType.asTypeName())
        annotation.elementValues.entries.forEachIndexed { i, (element, value) ->
            if (i > 0) {
                add(",")
            }
            add("\n")
            add("%S·to·", element.simpleName)
            add(value, element.returnType, imports)
            add("")
        }
        add("⇤\n))")
    }
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

data class GlobalConfig(
    val useAdaptersForPrimitives: Boolean,
    val serializeNulls: SerializeNulls
) {
    constructor(factory: KotshiJsonAdapterFactory) : this(
        useAdaptersForPrimitives = factory.useAdaptersForPrimitives,
        serializeNulls = factory.serializeNulls
    )

    companion object {
        val DEFAULT = GlobalConfig(false, SerializeNulls.DEFAULT)
    }
}

private val kotshiUtilsByteValue = KotshiUtils::class.import("byteValue")
private val kotshiUtilsValue = KotshiUtils::class.import("value")
private val kotshiUtilsNextFloat = KotshiUtils::class.import("nextFloat")
private val kotshiUtilsNextByte = KotshiUtils::class.import("nextByte")
private val kotshiUtilsNextShort = KotshiUtils::class.import("nextShort")
private val kotshiUtilsNextChar = KotshiUtils::class.import("nextChar")
private val kotshiUtilsAppendNullableError = KotshiUtils::class.import("appendNullableError")
private val kotshiUtilsCreateJsonQualifierImplementation = KotshiUtils::class.import("createJsonQualifierImplementation")
private val jsonDefaultValue = JsonDefaultValue::class.java.asClassName()
private val jsonDataException = JsonDataException::class.java.asClassName()
private val jsonReaderOptions = JsonReader.Options::class.java.asClassName()