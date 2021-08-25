package se.ansman.kotshi.renderer

import com.squareup.kotlinpoet.ARRAY
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.Dynamic
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.plusParameter
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.withIndent
import se.ansman.kotshi.Functions.Kotshi.value
import se.ansman.kotshi.Polymorphic
import se.ansman.kotshi.TypeRenderer
import se.ansman.kotshi.Types
import se.ansman.kotshi.Types.Moshi.jsonDataException
import se.ansman.kotshi.addControlFlow
import se.ansman.kotshi.addElse
import se.ansman.kotshi.addIf
import se.ansman.kotshi.addIfElse
import se.ansman.kotshi.addWhile
import se.ansman.kotshi.applyEachIndexed
import se.ansman.kotshi.applyIf
import se.ansman.kotshi.model.SealedClassJsonAdapter
import java.lang.reflect.ParameterizedType

class SealedClassAdapterRenderer(private val adapter: SealedClassJsonAdapter) : AdapterRenderer(adapter) {
    private val labelKeyOptions = PropertySpec
        .builder(nameAllocator.newName("labelKeyOptions"), Types.Moshi.jsonReaderOptions, KModifier.PRIVATE)
        .initializer("%T.of(%S)", Types.Moshi.jsonReaderOptions, adapter.labelKey)
        .build()

    private val adapterType = Types.Moshi.jsonAdapter.plusParameter(adapter.targetType)
    private val labels = adapter.subtypes.map { it.label }
    private val labelOptions = jsonOptionsProperty(labels)
    private val adapters by lazy {
        PropertySpec
            .builder(nameAllocator.newName("adapters"), ARRAY.plusParameter(adapterType), KModifier.PRIVATE)
            .initializer(
                CodeBlock.builder()
                    .add("arrayOf(«")
                    .applyEachIndexed(adapter.subtypes) { index, subtype ->
                        if (index > 0) {
                            add(",")
                        }
                        add("\nmoshi.adapter<%T>(", adapter.targetType)
                        add(subtype.render(forceBox = true))
                        add(")")
                    }
                    .add("\n»)\n")
                    .build())
            .build()
    }

    private val defaultAdapterIndex = adapter.subtypes.indexOfFirst { it.type == adapter.defaultType }
    private val defaultAdapterProperty = adapter.defaultType
        ?.takeIf { defaultAdapterIndex == -1 }
        ?.let { defaultType ->
            PropertySpec
                .builder(nameAllocator.newName("defaultAdapter"), adapterType, KModifier.PRIVATE)
                .apply {
                    if (defaultAdapterIndex == -1) {
                        initializer("moshi.adapter<%T>(%T::class.java)", adapter.targetType, defaultType)
                    } else {
                        initializer("%N[%L]", adapters, defaultAdapterIndex)
                    }
                }
                .build()
        }

    private val defaultAdapterAccessor = when {
        defaultAdapterIndex != -1 -> CodeBlock.of("%N[%L]", adapters, defaultAdapterIndex)
        defaultAdapterProperty != null -> CodeBlock.of("%N", defaultAdapterProperty)
        else -> null
    }

    init {
        with(nameAllocator) {
            newName("peek")
            newName("labelIndex")
            newName("adapter")
        }
    }

    override fun TypeSpec.Builder.renderSetup() {
        primaryConstructor(FunSpec.constructorBuilder()
            .addParameter("moshi", Types.Moshi.moshi)
            .applyIf(adapter.targetTypeVariables.isNotEmpty()) {
                addParameter("types", Types.Kotlin.array.parameterizedBy(Types.Java.type))
            }
            .build())
        addProperty(labelKeyOptions)
        addProperty(labelOptions)
        addProperty(adapters)

        if (defaultAdapterProperty != null) {
            addProperty(defaultAdapterProperty)
        }
    }

    override fun FunSpec.Builder.renderFromJson(readerParameter: ParameterSpec) {
        addIf(
            "%N.peek()·==·%T.NULL",
            readerParameter,
            Types.Moshi.jsonReaderToken
        ) {
            addStatement("return·%N.nextNull()", readerParameter)
        }
        addControlFlow("%N.peekJson().use·{·peek·->", readerParameter) {
            addStatement("peek.setFailOnUnknown(false)")
            addStatement("peek.beginObject()")
            addWhile("peek.hasNext()") {
                addIf("peek.selectName(%N)·==·-1", labelKeyOptions) {
                    addStatement("peek.skipName()")
                    addStatement("peek.skipValue()")
                    addStatement("continue")
                }
                addStatement("val·labelIndex·= peek.selectString(%N)", labelOptions)
                addControlFlow("val·adapter·= if·(labelIndex·==·-1)", close = false) {
                    if (adapter.onInvalid == Polymorphic.Fallback.FAIL || adapter.defaultType == null && adapter.onInvalid == Polymorphic.Fallback.DEFAULT) {
                        addStatement(
                            "throw·%T(%S)",
                            jsonDataException,
                            "Expected one of $labels for key '${adapter.labelKey}' but found \${peek.nextString()}"
                        )
                    } else if (adapter.onInvalid == Polymorphic.Fallback.NULL) {
                        addStatement("%N.skipValue()", readerParameter)
                        addStatement("return·null")
                    } else {
                        addStatement("%L", defaultAdapterAccessor!!)
                    }
                }
                addElse {
                    addStatement("adapters[labelIndex]")
                }
                addStatement("return·adapter.fromJson(%N)", readerParameter)
            }

            if (adapter.onMissing == Polymorphic.Fallback.FAIL || adapter.defaultType == null && adapter.onMissing == Polymorphic.Fallback.DEFAULT) {
                addStatement("throw·%T(%S)", jsonDataException, "Missing label for ${adapter.labelKey}")
            } else if (adapter.onMissing == Polymorphic.Fallback.NULL) {
                addStatement("%N.skipValue()", readerParameter)
                addStatement("return·null")
            } else {
                addStatement("return·%L.fromJson(%N)", defaultAdapterAccessor!!, readerParameter)
            }
        }
    }

    override fun FunSpec.Builder.renderToJson(writerParameter: ParameterSpec, valueParameter: ParameterSpec) {
        this
            .addIfElse("%N == null", value) {
                addStatement("%N.nullValue()", writerParameter)
            }
            .addElse {
                addControlFlow("val adapter = when (%N)", value) {
                    adapter.subtypes.forEachIndexed { index, subtype ->
                        addStatement("is %T·-> %N[%L]", subtype.wildcardType, adapters, index)
                    }
                    val defaultType = adapter.defaultType
                    if (defaultType != null && defaultAdapterIndex == -1) {
                        addStatement("is %T·-> %L", defaultType, checkNotNull(defaultAdapterAccessor))
                    }
                }
                addStatement("adapter.toJson(%N, %N)", writerParameter, value)
            }
    }

    private fun SealedClassJsonAdapter.Subtype.render(forceBox: Boolean = false): CodeBlock {
        val renderer = TypeRenderer { typeVariable ->
            val superParameters = (superClass as ParameterizedTypeName?)?.typeArguments

            fun TypeName.findAccessor(typesIndex: Int): CodeBlock? {
                return when (this) {
                    is ClassName,
                    Dynamic,
                    is LambdaTypeName -> null
                    is WildcardTypeName -> {
                        for (outType in outTypes) {
                            outType.findAccessor(typesIndex)?.let { return it }
                        }
                        for (inType in inTypes) {
                            inType.findAccessor(typesIndex)?.let { return it }
                        }
                        null
                    }
                    is TypeVariableName -> {
                        if (name.contentEquals(typeVariable.name)) {
                            CodeBlock.of("")
                        } else {
                            for (bound in bounds) {
                                bound.findAccessor(typesIndex)?.let { return it }
                            }
                            null
                        }
                    }
                    is ParameterizedTypeName -> {
                        typeArguments.forEachIndexed { index, typeName ->
                            val accessor = typeName.findAccessor(typesIndex) ?: return@forEachIndexed
                            return CodeBlock.builder()
                                .addControlFlow(".let") {
                                    add("it as? %T\n", ParameterizedType::class.java)
                                    withIndent {
                                        add(
                                            "?: throw %T(%P)\n",
                                            IllegalArgumentException::class.java,
                                            "The type \${types[$typesIndex]} is not a valid type constraint for the \$this"
                                        )
                                    }
                                }
                                .add(".actualTypeArguments[%L]", index)
                                .add(accessor)
                                .build()
                        }
                        null
                    }
                }
            }
            superParameters?.forEachIndexed { index, superParameter ->
                val accessor = superParameter.findAccessor(index) ?: return@forEachIndexed
                return@TypeRenderer CodeBlock.builder()
                    .add("types[%L]\n", index)
                    .withIndent {
                        add(accessor)
                    }
                    .build()
            }

            throw IllegalArgumentException("Could not determine type of type variable ${typeVariable.name} for type $type.")
        }

        return renderer.render(type, forceBox = forceBox)
    }
}