package se.ansman.kotshi

import com.google.auto.common.MoreElements
import com.google.common.collect.SetMultimap
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.NameAllocator
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import com.squareup.javapoet.TypeVariableName
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import java.io.IOException
import java.lang.reflect.Type
import java.util.*
import javax.annotation.processing.Filer
import javax.annotation.processing.Messager
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.NoType
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.ElementFilter
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import javax.tools.Diagnostic

class AdaptersProcessingStep(
    private val messager: Messager,
    private val types: Types,
    private val filer: Filer,
    private val adapters: MutableMap<TypeName, GeneratedAdapter>,
    private val defaultValueProviders: DefaultValueProviders,
    private val elements: Elements,
    private val sourceVersion: SourceVersion
) : KotshiProcessor.ProcessingStep {
    override val annotations: Set<Class<out Annotation>> =
        setOf(JsonSerializable::class.java, KotshiJsonAdapterFactory::class.java)

    override fun process(elementsByAnnotation: SetMultimap<Class<out Annotation>, Element>, roundEnv: RoundEnvironment) {
        val globalConfig = (elementsByAnnotation[KotshiJsonAdapterFactory::class.java]
            .firstOrNull()
            ?.getAnnotation(KotshiJsonAdapterFactory::class.java)
            ?.let { GlobalConfig(it) }
            ?: GlobalConfig.DEFAULT)

        for (element in elementsByAnnotation[JsonSerializable::class.java]) {
            try {
                generateJsonAdapter(globalConfig, element)
            } catch (e: ProcessingError) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Kotshi: ${e.message}", e.element)
            }
        }
    }

    private val Element.fields: List<VariableElement>
        get() {
            if (this !is TypeElement) {
                return emptyList()
            }

            val superFields = if (superclass is NoType) {
                emptyList()
            } else {
                types.asElement(superclass).fields
            }
            return superFields.plus(ElementFilter.fieldsIn(enclosedElements))
        }

    private val Element.methods: List<ExecutableElement>
        get() {
            if (this !is TypeElement) {
                return emptyList()
            }

            val superMethods = if (superclass is NoType) {
                emptyList()
            } else {
                types.asElement(superclass).methods
            }
            return superMethods.plus(ElementFilter.methodsIn(enclosedElements))
        }

    private fun generateJsonAdapter(globalConfig: GlobalConfig, element: Element) {
        val nameAllocator = NameAllocator()
        val typeElement = MoreElements.asType(element)
        val typeMirror = typeElement.asType()
        val typeName = TypeName.get(typeMirror)

        val constructor = findConstructor(element)

        val fields = element.fields.associateBy { it.simpleName.toString() }
        val methods = element.methods.associateBy { it.simpleName.toString() }

        val properties = constructor.parameters
            .map { parameter ->
                val field = fields[parameter.simpleName.toString()]

                val getterName = parameter.getAnnotation(GetterName::class.java)?.value ?: parameter.getGetterName()

                val getter = methods[getterName]

                if (getter != null && Modifier.PRIVATE in getter.modifiers) {
                    throw ProcessingError("Getter must not be private", getter)
                }

                if (getter == null) {
                    if (field == null) {
                        throw ProcessingError("Could not find a field named ${parameter.simpleName} or a getter named $getterName", parameter)
                    }
                    if (Modifier.PRIVATE in field.modifiers) {
                        throw ProcessingError("Could not find a getter named $getterName, annotate the parameter with @GetterName if you use @JvmName", parameter)
                    }
                }

                Property(
                    defaultValueProviders = defaultValueProviders,
                    types = types,
                    globalConfig = globalConfig,
                    enclosingClass = element,
                    parameter = parameter,
                    field = field,
                    getter = getter
                )
            }

        val adapterKeys: Set<AdapterKey> = properties
            .asSequence()
            .filter { it.shouldUseAdapter }
            .map { it.adapterKey }
            .toSet()

        val adapter = ClassName.bestGuess(typeElement.toString()).let {
            ClassName.get(it.packageName(), "Kotshi${it.simpleNames().joinToString("_")}JsonAdapter")
        }

        val genericTypes: List<TypeVariableName> = (typeName as? ParameterizedTypeName)
            ?.typeArguments
            ?.map { it as TypeVariableName }
            ?: emptyList()

        nameAllocator.newName("OPTIONS")
        nameAllocator.newName("value")
        nameAllocator.newName("writer")
        nameAllocator.newName("reader")
        nameAllocator.newName("stringBuilder")
        (0 until adapterKeys.size).forEach { nameAllocator.newName(generateAdapterFieldName(it)) }

        val jsonNames = properties
            .filterNot { it.isTransient }
            .map { it.jsonName }
        val stringArguments = Collections.nCopies(jsonNames.size, "\$S").joinToString(",\n")
        val optionsField = FieldSpec.builder(JsonReader.Options::class.java, "OPTIONS", Modifier.FINAL, Modifier.STATIC, Modifier.PRIVATE)
            .initializer("\$[\$T.of(\n$stringArguments)\$]", JsonReader.Options::class.java, *jsonNames.toTypedArray())
            .build()
        val typeSpec = TypeSpec.classBuilder(adapter)
            .addTypeVariables(genericTypes)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .superclass(getAdapterType(NamedJsonAdapter::class.java, typeName))
            .applyIf(jsonNames.isNotEmpty()) {
                addField(optionsField)
            }
            .addFields(generateFields(adapterKeys))
            .addMethod(generateConstructor(element, typeElement, adapterKeys, genericTypes))
            .addMethod(generateWriteMethod(typeMirror, properties, adapterKeys))
            .addMethod(generateReadMethod(nameAllocator, typeMirror, properties, adapterKeys, optionsField))
            .maybeAddGeneratedAnnotation(elements, sourceVersion)
            .build()

        val output = JavaFile.builder(adapter.packageName(), typeSpec).build()

        output.writeTo(filer)
        adapters[TypeName.get(element.asType())] = when {
            adapterKeys.isEmpty() -> GeneratedAdapter(adapter, requiresMoshi = false)
            genericTypes.isNotEmpty() -> GeneratedAdapter(adapter, requiresTypes = true)
            else -> GeneratedAdapter(adapter)
        }
    }

    private fun findConstructor(element: Element): ExecutableElement {
        val constructors = ElementFilter.constructorsIn(element.enclosedElements)

        if (constructors.isEmpty()) {
            throw ProcessingError("No constructors found", element)
        }

        fun ExecutableElement.validate(): ExecutableElement {
            if (Modifier.PRIVATE in modifiers) {
                throw ProcessingError("Constructor is private", this)
            }
            return this
        }

        val kotshiConstructors = constructors.filter { it.getAnnotation(KotshiConstructor::class.java) != null }
        val nonEmptyConstructors = constructors.filter { it.parameters.isNotEmpty() }

        val constructor = when {
            kotshiConstructors.size == 1 -> kotshiConstructors.first()
            kotshiConstructors.size > 1 -> throw ProcessingError("Multiple constructors annotated with @KotshiConstructor", element)
            nonEmptyConstructors.size == 1 -> constructors.first()
            constructors.size == 1 -> constructors.first()
            else -> throw ProcessingError("Multiple constructors found, please annotate the primary one with @KotshiConstructor", element)
        }
        return constructor.validate()
    }

    private fun generateFields(properties: Set<AdapterKey>): List<FieldSpec> =
        properties.mapIndexed { index, (type) ->
            FieldSpec
                .builder(getAdapterType(JsonAdapter::class.java, type),
                    generateAdapterFieldName(index),
                    Modifier.PRIVATE,
                    Modifier.FINAL)
                .build()
        }

    private fun generateConstructor(element: Element,
                                    typeElement: TypeElement,
                                    adapters: Set<AdapterKey>,
                                    genericTypes: List<TypeVariableName>): MethodSpec = MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PUBLIC)
        .applyIf(adapters.isNotEmpty()) {
            addParameter(Moshi::class.java, "moshi")
        }
        .applyIf(genericTypes.isNotEmpty()) {
            addParameter(Array<Type>::class.java, "types")
        }
        .addStatement("super(\$S)", ClassName.bestGuess(typeElement.toString())
            .simpleNames()
            .joinToString(".")
            .let { "KotshiJsonAdapter($it)" })
        .apply {
            fun AdapterKey.annotations(): CodeBlock = when (jsonQualifiers.size) {
                0 -> CodeBlock.of("")
                1 -> CodeBlock.of(", \$T.class", jsonQualifiers.first())
                else -> CodeBlock.builder()
                    .add(", \$T.unmodifiableSet(new \$T<>(\$T.asList(",
                        Collections::class.java, LinkedHashSet::class.java, Arrays::class.java)
                    .apply {
                        jsonQualifiers.forEachIndexed { index, qualifier ->
                            if (index > 0) add(", ")
                            add("\$T.createJsonQualifierImplementation(\$T.class)", KotshiUtils::class.java, qualifier)
                        }
                    }
                    .add(")))")
                    .build()
            }

            adapters.forEachIndexed { index, adapterKey ->
                val fieldName = generateAdapterFieldName(index)

                addCode(CodeBlock.builder()
                    .add("\$[\$L = moshi.adapter(", fieldName)
                    .add(adapterKey.asRuntimeType { typeVariableName ->
                        val genericIndex = genericTypes.indexOf(typeVariableName)
                        if (genericIndex == -1) {
                            throw ProcessingError("Element is generic but if an unknown type", element)
                        } else {
                            CodeBlock.of("types[$genericIndex]")
                        }
                    })
                    .add(adapterKey.annotations())
                    .add(");\$]\n")
                    .build())
            }
        }
        .build()

    private fun getAdapterType(superClass: Class<*>, typeName: TypeName): ParameterizedTypeName =
        ParameterizedTypeName.get(ClassName.get(superClass), typeName.box())

    private fun generateAdapterFieldName(index: Int): String = "adapter$index"

    private val Property.jsonType get() = if (this.type.isBoxedPrimitive) this.type.unbox() else this.type

    private fun generateWriteMethod(type: TypeMirror,
                                    properties: Collection<Property>,
                                    adapterKeys: Set<AdapterKey>): MethodSpec {
        val builder = MethodSpec.methodBuilder("toJson")
            .addAnnotation(Override::class.java)
            .addModifiers(Modifier.PUBLIC)
            .addException(IOException::class.java)
            .addParameter(JsonWriter::class.java, "writer")
            .addParameter(TypeName.get(type), "value")

        if (properties.isEmpty()) {
            builder
                .addIfElse("value == null") {
                    addStatement("writer.nullValue()")
                }
                .addElse {
                    addStatement("writer\n.beginObject()\n.endObject()")
                }

        } else {
            builder
                .addIf("value == null") {
                    addStatement("writer.nullValue()")
                    addStatement("return")
                }
                .addStatement("writer.beginObject()")
                .addCode("\n")
                .applyEach(properties.filterNot { it.isTransient }) { property ->
                    addStatement("writer.name(\$S)", property.jsonName)
                    val getter = if (property.getter != null) {
                        "value.${property.getter.simpleName}()"
                    } else {
                        "value.${property.field!!.simpleName}"
                    }

                    if (property.shouldUseAdapter) {
                        val adapterName = generateAdapterFieldName(adapterKeys.indexOf(property.adapterKey))
                        addStatement("$adapterName.toJson(writer, $getter)")
                    } else {
                        fun MethodSpec.Builder.writePrimitive(getter: String) =
                            when (property.jsonType) {
                                TYPE_NAME_STRING,
                                TypeName.INT,
                                TypeName.LONG,
                                TypeName.FLOAT,
                                TypeName.DOUBLE,
                                TypeName.SHORT,
                                TypeName.BOOLEAN -> addStatement("writer.value($getter)")
                                TypeName.BYTE -> addStatement("\$T.byteValue(writer, $getter)", KotshiUtils::class.java)
                                TypeName.CHAR -> addStatement("\$T.value(writer, $getter)", KotshiUtils::class.java)
                                else -> throw AssertionError("Unknown type ${property.type}")
                            }

                        writePrimitive(getter)
                    }
                }
                .addCode("\n")
                .addStatement("writer.endObject()")
        }
        return builder.build()
    }

    private fun generateReadMethod(nameAllocator: NameAllocator,
                                   type: TypeMirror,
                                   properties: Collection<Property>,
                                   adapters: Set<AdapterKey>,
                                   optionsField: FieldSpec): MethodSpec {
        val helperNames = mutableMapOf<Property, String>()
        val variableNames = mutableMapOf<Property, String>()
        fun Property.helperBooleanName() = helperNames.getOrPut(this) {
            nameAllocator.newName("${name}IsSet", "${name}IsSet")
        }

        fun Property.variableName() = variableNames.getOrPut(this) {
            nameAllocator.newName(name.toString(), this)
        }

        fun Property.variableType() = if (shouldUseAdapter && this.type.isPrimitive) this.type.box() else this.type

        fun Property.useStaticDefault() = defaultValueProvider?.isStatic == true && !shouldUseAdapter

        fun Property.requiresHelper() = !useStaticDefault() && variableType().isPrimitive


        val builder = MethodSpec.methodBuilder("fromJson")
            .addAnnotation(Override::class.java)
            .addModifiers(Modifier.PUBLIC)
            .addException(IOException::class.java)
            .addParameter(JsonReader::class.java, "reader")
            .returns(TypeName.get(type))

        if (properties.isEmpty()) {
            builder.addSwitch("reader.peek()") {
                builder.addSwitchBranch("NULL", terminator = null) {
                    addStatement("return reader.nextNull()")
                }
                builder.addSwitchBranch("BEGIN_OBJECT", terminator = null) {
                    addStatement("reader.skipValue()")
                    addStatement("return new \$T()", type)
                }
                builder.addSwitchDefault(terminator = null) {
                    addComment("Will throw an exception")
                    addStatement("reader.beginObject()")
                    addStatement("throw new \$T()", AssertionError::class.java)
                }
            }
            return builder.build()
        }

        return builder
            .addIf("reader.peek() == \$T.NULL", JsonReader.Token::class.java) {
                addStatement("return reader.nextNull()")
            }
            .addCode("\n")
            .addStatement("reader.beginObject()")
            .addCode("\n")
            .applyEach(properties) { property ->
                val variableType = property.variableType()
                if (property.requiresHelper()) {
                    addStatement("boolean \$L = false", property.helperBooleanName())
                }
                if (property.useStaticDefault()) {
                    addCode(CodeBlock.builder()
                        .add("$[")
                        .add("\$T \$N = ", variableType, property.variableName())
                        .add(property.defaultValueProvider!!.accessor)
                        .add(";\n$]")
                        .build())
                } else {
                    addStatement("\$T \$N = \$L", variableType, property.variableName(), variableType.jvmDefault)
                }
            }
            .addWhile("reader.hasNext()") {
                addSwitch("reader.selectName(\$N)", optionsField) {
                    properties.filterNot { it.isTransient }.forEachIndexed { index, property ->
                        addSwitchBranch("\$L", index, terminator = "continue") {
                            if (property.shouldUseAdapter) {
                                val adapterFieldName = generateAdapterFieldName(adapters.indexOf(property.adapterKey))
                                addStatement("\$L = \$L.fromJson(reader)", property.variableName(), adapterFieldName)
                            } else {
                                fun MethodSpec.Builder.readPrimitive(reader: () -> Unit) {
                                    addIfElse("reader.peek() == \$T.NULL", JsonReader.Token::class.java) {
                                        addStatement("reader.nextNull()")
                                    }
                                    addElse {
                                        reader()
                                        if (property.requiresHelper()) {
                                            addStatement("\$L = true", property.helperBooleanName())
                                        }
                                    }
                                }

                                when (property.jsonType) {
                                    TYPE_NAME_STRING -> readPrimitive {
                                        addStatement("\$L = reader.nextString()", property.variableName())
                                    }
                                    TypeName.BOOLEAN -> readPrimitive {
                                        addStatement("\$L = reader.nextBoolean()", property.variableName())
                                    }
                                    TypeName.BYTE -> readPrimitive {
                                        addStatement("\$L = \$T.nextByte(reader)", property.variableName(), KotshiUtils::class.java)
                                    }
                                    TypeName.SHORT -> readPrimitive {
                                        addStatement("\$L = \$T.nextShort(reader)", property.variableName(), KotshiUtils::class.java)
                                    }
                                    TypeName.INT -> readPrimitive {
                                        addStatement("\$L = reader.nextInt()", property.variableName())
                                    }
                                    TypeName.LONG -> readPrimitive {
                                        addStatement("\$L = reader.nextLong()", property.variableName())
                                    }
                                    TypeName.CHAR -> readPrimitive {
                                        addStatement("\$L = \$T.nextChar(reader)", property.variableName(), KotshiUtils::class.java)
                                    }
                                    TypeName.FLOAT -> readPrimitive {
                                        addStatement("\$L = \$T.nextFloat(reader)", property.variableName(), KotshiUtils::class.java)
                                    }
                                    TypeName.DOUBLE -> readPrimitive {
                                        addStatement("\$L = reader.nextDouble()", property.variableName())
                                    }
                                }
                            }
                        }
                    }
                    addSwitchBranch("-1", terminator = "continue") {
                        addStatement("reader.nextName()")
                        addStatement("reader.skipValue()")
                    }
                }
            }
            .addCode("\n")
            .addStatement("reader.endObject()")
            .apply {
                var hasStringBuilder = false
                for (property in properties) {
                    val variableType = property.variableType()
                    val check = if (variableType.isPrimitive) {
                        "!${property.helperBooleanName()}"
                    } else {
                        "${property.variableName()} == null"
                    }

                    if (!hasStringBuilder) {
                        val needsStringBuilder = if (property.defaultValueProvider != null) {
                            !variableType.isPrimitive && property.defaultValueProvider.isNullable && !property.defaultValueProvider.isStatic
                        } else {
                            !property.isNullable
                        }

                        if (needsStringBuilder) {
                            addStatement("\$T stringBuilder = null", StringBuilder::class.java)
                            hasStringBuilder = true
                        }
                    }

                    fun appendError() {
                        addStatement("stringBuilder = \$T.appendNullableError(stringBuilder, \$S)", KotshiUtils::class.java, property.name)
                    }

                    if (property.useStaticDefault()) {
                        // Empty
                    } else if (property.defaultValueProvider != null) {
                        addIf(check) {
                            // We require a temp var if the variable is a primitive and we allow the provider to return null
                            val requiresTmpVar = variableType.isPrimitive && property.defaultValueProvider.isNullable
                            val variableName = if (requiresTmpVar) {
                                nameAllocator.newName("${property.variableName()}Default")
                            } else {
                                property.variableName()
                            }
                            addCode("\$[")
                            if (requiresTmpVar) {
                                addCode("\$T $variableName = ", property.defaultValueProvider.type)
                            } else {
                                addCode("$variableName = ")
                            }
                            addCode(property.defaultValueProvider.accessor)
                            addCode(";\n\$]")

                            // If the variable we're assigning to is primitive we don't need any checks since java
                            // throws and if the default value provider cannot return null we don't need a check
                            if (!variableType.isPrimitive && property.defaultValueProvider.canReturnNull) {
                                if (!property.defaultValueProvider.isNullable) {
                                    addIf("$variableName == null") {
                                        addStatement("throw new \$T(\"The default value provider returned null\")",
                                            java.lang.NullPointerException::class.java)
                                    }
                                } else if (!property.isNullable) {
                                    addIf("$variableName == null") {
                                        appendError()
                                    }
                                }
                            }

                            // If we used a temp var we need to assign it to the real variable
                            if (requiresTmpVar) {
                                addStatement("${property.variableName()} = $variableName")
                            }

                        }
                    } else if (!property.isNullable) {
                        addIf(check) {
                            appendError()
                        }
                    }
                }
                if (hasStringBuilder) {
                    addIf("stringBuilder != null") {
                        addStatement("throw new \$T(stringBuilder.toString())", NullPointerException::class.java)
                    }
                    addCode("\n")
                }
            }
            .addStatement("return new \$T(\n${properties.joinToString(",\n") { it.variableName() }})", type)
            .build()
    }
}

data class GlobalConfig(
    val useAdaptersForPrimitives: Boolean
) {
    constructor(factory: KotshiJsonAdapterFactory) : this(factory.useAdaptersForPrimitives)

    companion object {
        val DEFAULT = GlobalConfig(false)
    }
}