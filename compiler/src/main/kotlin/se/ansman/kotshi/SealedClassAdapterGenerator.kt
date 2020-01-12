package se.ansman.kotshi

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.plusParameter
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.jvm.throws
import com.squareup.kotlinpoet.metadata.ImmutableKmClass
import com.squareup.kotlinpoet.metadata.isSealed
import com.squareup.kotlinpoet.metadata.specs.ClassInspector
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.Moshi
import java.io.IOException
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements

class SealedClassAdapterGenerator(
    classInspector: ClassInspector,
    elements: Elements,
    element: TypeElement,
    metadata: ImmutableKmClass,
    globalConfig: GlobalConfig
) : AdapterGenerator(classInspector, elements, element, metadata, globalConfig) {
    init {
        require(metadata.isSealed)
        if (metadata.typeParameters.isNotEmpty()) {
            throw ProcessingError("Generic sealed classes are not supported yet", element)
        }
    }

    private val labelKey = element.getAnnotation(Polymorphic::class.java)?.labelKey
        ?: throw ProcessingError("Sealed classes must be annotated with @Polymorphic", element)

    private val labelOptions = PropertySpec
        .builder(nameAllocator.newName("labelKeyOptions"), JsonReader.Options::class.java, KModifier.PRIVATE)
        .initializer("%T.of(%S)", jsonReaderOptions, labelKey)
        .build()

    override fun TypeSpec.Builder.addMethods(): Collection<String> {

        val implementations = metadata.sealedSubclasses
            .asSequence()
            .map {
                requireNotNull(elements.getTypeElement(it.replace('/', '.'))) {
                    "Could not find element for class ${it.replace('/', '.')}"
                }
            }
            .onEach {
                if (it.getAnnotation(JsonSerializable::class.java) == null) {
                    throw ProcessingError("All subclasses of a sealed class must be @JsonSerializable", it)
                }
                if (it.typeParameters.isNotEmpty()) {
                    throw ProcessingError("Generic sealed class implementations are not supported", it)
                }
                if (it.modifiers.contains(Modifier.ABSTRACT)) {
                    throw ProcessingError("Nested sealed classes are not supported yet", it)
                }
                if (it.getAnnotation(PolymorphicLabel::class.java) == null && it.getAnnotation(JsonDefaultValue::class.java) == null) {
                    throw ProcessingError("Subclasses of sealed classes must be annotated with @PolymorphicLabel or @JsonDefaultValue", it)
                }
            }
            .toList()

        val subtypes = implementations
            .asSequence()
            .mapNotNull {
                Subtype(
                    type = it,
                    label = it.getAnnotation(PolymorphicLabel::class.java)
                        ?.value
                        ?: return@mapNotNull null
                )
            }
            .toList()

        val labels = subtypes.map { it.label }

        for ((label, types) in subtypes.groupBy { it.label }.entries) {
            if (types.size != 1) {
                throw ProcessingError("@PolymorphicLabel $label found on multiple classes", types.first().type)
            }
        }

        val defaultType = implementations
            .filter { it.getAnnotation(JsonDefaultValue::class.java) != null }
            .let {
                when (it.size) {
                    0 -> null
                    1 -> it.single()
                    else -> throw ProcessingError("Multiple classes annotated with @JsonDefaultValue", it.first())
                }
            }

        val peekLabelIndex = FunSpec.builder("peeklabelIndex")
            .addModifiers(KModifier.PRIVATE)
            .receiver(JsonReader::class.java)
            .returns(Int::class.javaPrimitiveType!!)
            .addControlFlow("return peekJson().use { reader ->") {
                addStatement("reader.beginObject()")
                addWhile("reader.hasNext()") {
                    addIf("reader.selectName(%N) == -1", labelOptions) {
                        addStatement("reader.skipName()")
                        addStatement("reader.skipValue()")
                        addStatement("continue")
                    }
                    if (defaultType == null) {
                        addStatement("val labelIndex = reader.selectString(options)")
                        addIf("labelIndex == -1") {
                            addStatement("throw·%T(%S + reader.nextString())", jsonDataException, "Expected one of $labels for key '$labelKey' but found ")
                        }
                        addStatement("return·labelIndex")
                    } else {
                        addStatement("return·reader.selectString(options)")
                    }
                }
                if (defaultType == null) {
                    addStatement("throw·%T(%S)", jsonDataException, "Missing label for $labelKey")
                } else {
                    addStatement("-1")
                }
            }
            .build()

        val moshi = ParameterSpec.builder("moshi", Moshi::class.java).build()

        val adapterType = JsonAdapter::class.java.asClassName().plusParameter(typeName)
        val adapters = PropertySpec.builder(nameAllocator.newName("adapters"), LIST.plusParameter(adapterType), KModifier.PRIVATE)
            .initializer(CodeBlock.builder()
                .add("«listOf(")
                .applyEachIndexed(subtypes) { index, subtype ->
                    if (index > 0) {
                        add(",")
                    }
                    add("\n%N.adapter<%T>(%T::class.java)", moshi, typeName, subtype.className)
                }
                .add("\n)»")
                .build())
            .build()

        val defaultAdapter = if (defaultType == null) {
            null
        } else {
            val defaultIndex = subtypes.indexOfFirst { it.type == defaultType }
            if (defaultIndex == -1) {
                val defaultAdapter = PropertySpec.builder(nameAllocator.newName("defaultAdapter"), adapterType, KModifier.PRIVATE)
                    .initializer("moshi.adapter<%T>(%T::class.java)", typeName, defaultType.asClassName())
                    .build()
                addProperty(defaultAdapter)
                CodeBlock.of("%N", defaultAdapter)
            } else {
                CodeBlock.of("adapters[%L]", defaultIndex)
            }
        }

        this
            .primaryConstructor(FunSpec.constructorBuilder().addParameter(moshi).build())
            .addProperty(labelOptions)
            .addProperty(adapters)
            .addFunction(FunSpec.builder("toJson")
                .addModifiers(KModifier.OVERRIDE)
                .throws(IOException::class.java)
                .addParameter(writer)
                .addParameter(value)
                .addIfElse("%N == null", value) {
                    addStatement("%N.nullValue()", writer)
                }
                .addElse {
                    addControlFlow("val adapter = when (%N)", value) {
                        subtypes.forEachIndexed { index, subtype ->
                            addStatement("is %T·-> %N[%L]", subtype.className, adapters, index)
                        }
                        if (defaultAdapter != null && defaultType != null && subtypes.none { it.type == defaultType }) {
                            addStatement("is %T·-> %L", defaultType, defaultAdapter)
                        }
                    }
                    addStatement("adapter.toJson(%N, %N)", writer, value)
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
                .addNextControlFlow("else") {
                    addStatement("val·labelIndex·=·%N.%N()", reader, peekLabelIndex)
                    if (defaultAdapter != null) {
                        addStatement("val·adapter·=·if·(labelIndex·==·-1)·%L·else·adapters[labelIndex]", defaultAdapter)
                    } else {
                        addStatement("val·adapter·=·adapters[labelIndex]")
                    }
                    addStatement("adapter.fromJson(%N)", reader)
                }
                .build())
            .addFunction(peekLabelIndex)
        return labels
    }

    private data class Subtype(
        val type: TypeElement,
        val label: String
    ) {
        val className = type.asClassName()
    }
}