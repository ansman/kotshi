package se.ansman.kotshi.generators

import com.google.auto.common.MoreTypes
import com.squareup.kotlinpoet.ARRAY
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.plusParameter
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.jvm.throws
import com.squareup.kotlinpoet.metadata.ImmutableKmClass
import com.squareup.kotlinpoet.metadata.isSealed
import com.squareup.kotlinpoet.metadata.specs.ClassInspector
import com.squareup.kotlinpoet.metadata.toImmutableKmClass
import se.ansman.kotshi.JsonDefaultValue
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.Polymorphic
import se.ansman.kotshi.PolymorphicLabel
import se.ansman.kotshi.ProcessingError
import se.ansman.kotshi.addControlFlow
import se.ansman.kotshi.addElse
import se.ansman.kotshi.addIf
import se.ansman.kotshi.addIfElse
import se.ansman.kotshi.addNextControlFlow
import se.ansman.kotshi.addWhile
import se.ansman.kotshi.applyEachIndexed
import se.ansman.kotshi.metadata
import se.ansman.kotshi.nullable
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
        if (metadata.sealedSubclasses.isEmpty()) {
            throw ProcessingError("Sealed classes without implementations are not supported", element)
        }
    }

    private val labelKey = element.getAnnotation(Polymorphic::class.java)
        ?.labelKey
        ?: throw ProcessingError("Sealed classes must be annotated with @Polymorphic", element)

    private val labelKeyOptions = PropertySpec
        .builder(nameAllocator.newName("labelKeyOptions"), jsonReaderOptions, KModifier.PRIVATE)
        .addAnnotation(jvmStatic)
        .initializer("%T.of(%S)", jsonReaderOptions, labelKey)
        .build()

    override fun TypeSpec.Builder.addMethods() {
        val implementations = metadata.findSealedClassImplementations(className).toList()

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
            .receiver(jsonReader)
            .returns(INT)
            .addControlFlow("return peekJson().use { reader ->") {
                addStatement("reader.setFailOnUnknown(false)")
                addStatement("reader.beginObject()")
                addWhile("reader.hasNext()") {
                    addIf("reader.selectName(%N) == -1", labelKeyOptions) {
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

        val adapterType = jsonAdapter.plusParameter(typeName)
        val adapters = PropertySpec.builder(nameAllocator.newName("adapters"), ARRAY.plusParameter(adapterType), KModifier.PRIVATE)
            .initializer(CodeBlock.builder()
                .add("arrayOf(«")
                .applyEachIndexed(subtypes) { index, subtype ->
                    if (index > 0) {
                        add(",")
                    }
                    add("\n%N.adapter<%T>(%T::class.java)", moshiParameter, typeName, subtype.className)
                }
                .add("»\n)\n")
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
            .primaryConstructor(FunSpec.constructorBuilder().addParameter(moshiParameter).build())
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
                            addStatement("is %T·-> %N[%L]", subtype.className, adapters, index)
                        }
                        if (defaultAdapter != null && defaultType != null && subtypes.none { it.type == defaultType }) {
                            addStatement("is %T·-> %L", defaultType, defaultAdapter)
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
                .addControlFlow("return·if (%N.peek() == %T.NULL)", readerParameter, jsonReaderToken, close = false) {
                    addStatement("%N.nextNull()", readerParameter)
                }
                .addNextControlFlow("else") {
                    addStatement("val·labelIndex·=·%N.%N()", readerParameter, peekLabelIndex)
                    if (defaultAdapter != null) {
                        addStatement("val·adapter·=·if·(labelIndex·==·-1)·%L·else·adapters[labelIndex]", defaultAdapter)
                    } else {
                        addStatement("val·adapter·=·adapters[labelIndex]")
                    }
                    addStatement("adapter.fromJson(%N)", readerParameter)
                }
                .build())
            .addFunction(peekLabelIndex)
            .addType(TypeSpec.companionObjectBuilder()
                .addOptions(labels)
                .addProperty(labelKeyOptions)
                .build())
    }

    private fun ImmutableKmClass.findSealedClassImplementations(supertype: TypeName): Sequence<TypeElement> =
        sealedSubclasses
            .asSequence()
            .map {
                requireNotNull(elements.getTypeElement(it.replace('/', '.'))) {
                    "Could not find element for class $it"
                }
            }
            .filter { MoreTypes.asTypeElement(it.superclass).asClassName() == supertype }
            .onEach {
                if (it.getAnnotation(JsonSerializable::class.java) == null) {
                    throw ProcessingError("All subclasses of a sealed class must be @JsonSerializable", it)
                }
                if (it.typeParameters.isNotEmpty()) {
                    throw ProcessingError("Generic sealed class implementations are not supported", it)
                }
            }
            .flatMap {
                if (Modifier.ABSTRACT in it.modifiers) {
                    val kmClass = it.metadata.toImmutableKmClass()
                    if (!kmClass.isSealed) {
                        throw ProcessingError("Abstract implementations of sealed classes are not allowed", it)
                    }
                    val polymorphic = it.getAnnotation(Polymorphic::class.java)
                        ?: throw ProcessingError("Children of a sealed class must be annotated with @Polymorphic", it)
                    val polymorphicLabel = it.getAnnotation(PolymorphicLabel::class.java)
                    when {
                        polymorphic.labelKey == labelKey -> {
                            if (polymorphicLabel != null) {
                                throw ProcessingError("Children of a sealed class with the same label key must not be annotated with @PolymorphicLabel", it)
                            }
                            kmClass.findSealedClassImplementations(it.asClassName())
                        }
                        polymorphicLabel == null -> {
                            throw ProcessingError("Children of a sealed class with a different label key must be annotated with @PolymorphicLabel", it)
                        }
                        else -> sequenceOf(it)
                    }
                } else {
                    if (it.getAnnotation(PolymorphicLabel::class.java) == null && it.getAnnotation(JsonDefaultValue::class.java) == null) {
                        throw ProcessingError("Subclasses of sealed classes must be annotated with @PolymorphicLabel or @JsonDefaultValue", it)
                    }
                    sequenceOf(it)
                }
            }

    private data class Subtype(
        val type: TypeElement,
        val label: String
    ) {
        val className = type.asClassName()
    }
}