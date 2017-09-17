package se.ansman.kotshi

import com.google.auto.common.BasicAnnotationProcessor
import com.google.auto.common.MoreElements
import com.google.common.collect.SetMultimap
import com.squareup.javapoet.*
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import java.io.IOException
import java.lang.reflect.Type
import java.util.*
import javax.annotation.processing.Filer
import javax.annotation.processing.Messager
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.ElementFilter
import javax.tools.Diagnostic

class AdaptersProcessingStep(
        val messager: Messager,
        val filer: Filer,
        val adapters: MutableMap<TypeName, TypeName>
) : BasicAnnotationProcessor.ProcessingStep {
    override fun annotations(): Set<Class<out Annotation>> = setOf(JsonSerializable::class.java)

    override fun process(elementsByAnnotation: SetMultimap<Class<out Annotation>, Element>): Set<Element> {
        for (element in elementsByAnnotation[JsonSerializable::class.java]) {
            try {
                generateJsonAdapter(element)
            } catch (e: ProcessingError) {
                messager.printMessage(Diagnostic.Kind.ERROR, e.message, e.element)
            }
        }
        return emptySet()
    }

    private fun generateJsonAdapter(element: Element) {
        val typeElement = MoreElements.asType(element)
        val typeMirror = typeElement.asType()
        val typeName = TypeName.get(typeMirror)

        val constructor = findConstructor(element)

        val fields = ElementFilter.fieldsIn(element.enclosedElements).associateBy { it.simpleName.toString() }
        val methods = ElementFilter.methodsIn(element.enclosedElements).associateBy { it.simpleName.toString() }

        val properties = constructor.parameters
                .map { parameter ->
                    val field = fields[parameter.simpleName.toString()]
                            ?: throw ProcessingError("Could not find a field name ${parameter.simpleName}", parameter)

                    val getterName = parameter.getAnnotation(GetterName::class.java)?.value ?: if (parameter.simpleName.startsWith("is")) {
                        parameter.simpleName.toString()
                    } else {
                        "get${parameter.simpleName.toString().capitalize()}"
                    }

                    val getter = methods[getterName]

                    if (getter != null && Modifier.PRIVATE in getter.modifiers) {
                        throw ProcessingError("Getter must not be private", getter)
                    }

                    if (getter == null && Modifier.PRIVATE in field.modifiers) {
                        throw ProcessingError("Could not find a getter named $getterName, annotate the parameter with @GetterName if you use @JvmName", parameter)
                    }

                    Property(parameter, field, getter)
                }

        val adapterKeys: Set<AdapterKey> = properties.mapTo(mutableSetOf()) { it.adapterKey }

        val adapter = ClassName.bestGuess(typeElement.toString()).let {
            ClassName.get(it.packageName(), "Kotshi${it.simpleNames().joinToString("_")}JsonAdapter")
        }

        val genericTypes: List<TypeVariableName> = (typeName as? ParameterizedTypeName)
                ?.typeArguments
                ?.map { it as TypeVariableName }
                ?: emptyList()

        val optionsField = FieldSpec.builder(JsonReader.Options::class.java, "OPTIONS", Modifier.FINAL, Modifier.STATIC, Modifier.PRIVATE)
                .initializer("\$T.of(${properties.joinToString(", ") { "\"${it.jsonName}\"" }})", JsonReader.Options::class.java)
                .build()
        val typeSpec = TypeSpec.classBuilder(adapter)
                .addTypeVariables(genericTypes)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .superclass(getAdapterType(typeName))
                .addField(optionsField)
                .addFields(generateFields(adapterKeys))
                .addMethod(generateConstructor(adapterKeys, genericTypes))
                .addMethod(generateWriteMethod(typeMirror, properties, adapterKeys))
                .addMethod(generateReadMethod(typeMirror, properties, adapterKeys, optionsField))
                .build()

        val output = JavaFile.builder(adapter.packageName(), typeSpec).build()

        output.writeTo(filer)
        adapters[TypeName.get(element.asType())] = if (genericTypes.isEmpty()) {
            adapter
        } else {
            ParameterizedTypeName.get(adapter, *(genericTypes as List<TypeName>).toTypedArray())
        }
    }

    private fun findConstructor(element: Element): ExecutableElement {
        val constructors = ElementFilter.constructorsIn(element.enclosedElements)
                .filter { it.parameters.isNotEmpty() }

        if (constructors.isEmpty()) {
            throw ProcessingError("No non empty constructor found", element)
        }

        fun ExecutableElement.validate() {
            if (Modifier.PRIVATE in modifiers) {
                throw ProcessingError("Constructor is private", this)
            }
            if (parameters.isEmpty()) {
                throw ProcessingError("Constructor has no parameters", this)
            }
        }

        return if (constructors.size == 1) {
            constructors.first()
        } else {
            constructors
                    .filter { it.getAnnotation(KotshiConstructor::class.java) != null }
                    .also {
                        if (it.isEmpty()) {
                            throw ProcessingError("Multiple constructors found, please annotate the primary one with @KotshiConstructor", element)
                        } else if (it.size > 1) {
                            throw ProcessingError("Multiple constructors annotated with @KotshiConstructor", element)
                        }
                    }
                    .first()
        }.also { it.validate() }
    }

    private fun generateFields(properties: Set<AdapterKey>): List<FieldSpec> =
            properties.mapIndexed { index, (type) ->
                FieldSpec
                        .builder(getAdapterType(type),
                                generateAdapterFieldName(index),
                                Modifier.PRIVATE,
                                Modifier.FINAL)
                        .build()
            }

    private fun generateConstructor(adapters: Set<AdapterKey>,
                                    genericTypes: List<TypeVariableName>): MethodSpec = MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PUBLIC)
            .addParameter(Moshi::class.java, "moshi")
            .apply {
                if (genericTypes.isNotEmpty()) {
                    addParameter(Array<Type>::class.java, "types")
                }
            }
            .apply {
                fun AdapterKey.annotations(): CodeBlock = when (jsonQualifiers.size) {
                    0 -> CodeBlock.of("")
                    1 -> CodeBlock.of(", \$T.class", jsonQualifiers.first())
                    else -> CodeBlock.builder()
                            .add(", new \$T(\$T.asList(", LinkedHashSet::class.java, Arrays::class.java)
                            .apply {
                                jsonQualifiers.forEachIndexed { index, qualifier ->
                                    if (index > 0) add(", ")
                                    add("\$T.createJsonQualifierImplementation(\$T.class)", KotshiUtils::class.java, qualifier)
                                }
                            }
                            .add("))")
                            .build()
                }

                adapters.forEachIndexed { index, adapterKey ->
                    val fieldName = generateAdapterFieldName(index)
                    if (adapterKey.isGeneric) {
                        val genericIndex = genericTypes.indexOf(adapterKey.type)
                        if (genericIndex == -1) {
                            messager.printMessage(Diagnostic.Kind.ERROR, "Element is generic but if an unknown type")
                            return@forEachIndexed
                        }
                        addCode(CodeBlock.builder()
                                .add("\$[\$L = moshi.adapter(types[$genericIndex]", fieldName)
                                .add(adapterKey.annotations())
                                .add(");\$]\n")
                                .build())
                    } else {
                        addCode(CodeBlock.builder()
                                .add("\$[\$L = moshi.adapter(", fieldName)
                                .add(adapterKey.asRuntimeType())
                                .add(adapterKey.annotations())
                                .add(");\$]\n")
                                .build())
                    }
                }
            }
            .build()

    private fun getAdapterType(typeName: TypeName): ParameterizedTypeName =
            ParameterizedTypeName.get(ClassName.get(JsonAdapter::class.java), typeName.box())

    private fun generateAdapterFieldName(index: Int): String = "adapter$index"

    private fun generateWriteMethod(type: TypeMirror,
                                    properties: Iterable<Property>,
                                    adapterKeys: Set<AdapterKey>): MethodSpec =
            MethodSpec.methodBuilder("toJson")
                    .addAnnotation(Override::class.java)
                    .addModifiers(Modifier.PUBLIC)
                    .addException(IOException::class.java)
                    .addParameter(JsonWriter::class.java, "writer")
                    .addParameter(TypeName.get(type), "value")
                    .addIfElse("value == null") {
                        addStatement("writer.nullValue()")
                    }
                    .addElse {
                        addStatement("writer.beginObject()")
                        for (property in properties) {
                            addStatement("writer.name(\$S)", property.jsonName)
                            val adapterFieldName = generateAdapterFieldName(adapterKeys.indexOf(property.adapterKey))
                            if (property.getter == null) {
                                addStatement("\$L.toJson(writer, value.\$N)", adapterFieldName, property.field.simpleName)
                            } else {
                                addStatement("\$L.toJson(writer, value.\$N())", adapterFieldName, property.getter.simpleName)
                            }
                        }
                        addStatement("writer.endObject()")
                    }
                    .build()

    private fun generateReadMethod(type: TypeMirror,
                                   properties: Iterable<Property>,
                                   adapters: Set<AdapterKey>,
                                   optionsField: FieldSpec): MethodSpec = MethodSpec.methodBuilder("fromJson")
            .addAnnotation(Override::class.java)
            .addModifiers(Modifier.PUBLIC)
            .addException(IOException::class.java)
            .addParameter(JsonReader::class.java, "reader")
            .returns(TypeName.get(type))
            .addIf("reader.peek() == \$T.NULL", JsonReader.Token::class.java) {
                addStatement("return reader.nextNull()")
            }
            .apply {
                for (property in properties) {
                    addStatement("\$T \$N = null", property.type.box(), property.name)
                }
            }
            .addStatement("reader.beginObject()")
            .addWhile("reader.hasNext()") {
                addSwitch("reader.selectName(\$N)", optionsField) {
                    properties.forEachIndexed { index, property ->
                        addSwitchBranch("case \$L", index) {
                            addStatement("\$L = \$L.fromJson(reader)",
                                    property.name,
                                    generateAdapterFieldName(adapters.indexOf(property.adapterKey)))
                        }
                    }
                    addSwitchBranch("case -1") {
                        addStatement("reader.nextName()")
                        addStatement("reader.skipValue()")
                    }
                }
            }
            .addStatement("reader.endObject()")
            .apply {
                properties.filterNot { it.isNullable }.takeIf { it.isNotEmpty() }?.let {
                    addStatement("\$T stringBuilder = null", StringBuilder::class.java)
                    for (property in it) {
                        addIf("\$L == null", property.name) {
                            addStatement("stringBuilder = \$T.appendNullableError(stringBuilder, \$S)", KotshiUtils::class.java, property.name)
                        }
                    }
                    addIf("stringBuilder != null") {
                        addStatement("throw new \$T(stringBuilder.toString())", NullPointerException::class.java)
                    }
                }
            }
            .addStatement("return new \$T(\n${properties.joinToString(", \n") { it.name }})", type)
            .build()

}

