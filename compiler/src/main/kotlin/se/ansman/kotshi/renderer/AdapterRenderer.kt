package se.ansman.kotshi.renderer

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.NameAllocator
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.plusParameter
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.jvm.throws
import se.ansman.kotshi.ProguardConfig
import se.ansman.kotshi.Types
import se.ansman.kotshi.Types.Moshi.jsonReaderOptions
import se.ansman.kotshi.applyEachIndexed
import se.ansman.kotshi.applyIf
import se.ansman.kotshi.model.DataClassJsonAdapter
import se.ansman.kotshi.model.EnumJsonAdapter
import se.ansman.kotshi.model.GeneratableJsonAdapter
import se.ansman.kotshi.model.GeneratedAdapter
import se.ansman.kotshi.model.GeneratedAnnotation
import se.ansman.kotshi.model.ObjectJsonAdapter
import se.ansman.kotshi.model.SealedClassJsonAdapter
import se.ansman.kotshi.nullable
import se.ansman.kotshi.withoutVariance

abstract class AdapterRenderer(private val adapter: GeneratableJsonAdapter) {
    private var isUsed = false
    protected val nameAllocator: NameAllocator = NameAllocator().apply {
        newName(moshiParameterName)
        newName(typesParameterName)
        newName("value")
        newName("writer")
        newName("reader")
        newName("stringBuilder")
        newName("it")
    }

    protected open fun TypeSpec.createProguardRule(): ProguardConfig? = null

    fun <OE> render(
        generatedAnnotation: GeneratedAnnotation?,
        originatingElement: OE,
        typeSpecModifier: TypeSpec.Builder.() -> Unit = {}
    ): GeneratedAdapter<OE> {
        check(!isUsed)
        isUsed = true
        val value = ParameterSpec.builder("value", adapter.targetType.nullable()).build()
        val writer = ParameterSpec.builder("writer", Types.Moshi.jsonWriter).build()
        val reader = ParameterSpec.builder("reader", Types.Moshi.jsonReader).build()
        val typeSpec = TypeSpec.classBuilder(adapter.adapterName)
            .addModifiers(KModifier.INTERNAL)
            .apply { generatedAnnotation?.toAnnotationSpec()?.let(::addAnnotation) }
            .addAnnotation(Types.Kotshi.internalKotshiApi)
            .addAnnotation(AnnotationSpec.builder(Types.Kotlin.suppress)
                // https://github.com/square/moshi/issues/1023
                .addMember("%S", "DEPRECATION")
                // Because we look it up reflectively
                .addMember("%S", "unused")
                // Because we include underscores
                .addMember("%S", "ClassName")
                // Because we generate redundant `out` variance for some generics and there's no way
                // for us to know when it's redundant.
                .addMember("%S", "REDUNDANT_PROJECTION")
                // Because we may generate redundant explicit types for local vars with default values.
                // Example: 'var fooSet: Boolean = false'
                .addMember("%S", "RedundantExplicitType")
                // NameAllocator will just add underscores to differentiate names, which Kotlin doesn't
                // like for stylistic reasons.
                .addMember("%S", "LocalVariableName")
                // KotlinPoet always generates explicit public modifiers for public members.
                .addMember("%S", "RedundantVisibilityModifier")
                // For LambdaTypeNames we have to import kotlin.functions.* types
                .addMember("%S", "PLATFORM_CLASS_MAPPED_TO_KOTLIN")
                // Cover for calling fromJson() on a Nothing property type. Theoretically nonsensical but we
                // support it
                .addMember("%S", "IMPLICIT_NOTHING_TYPE_ARGUMENT_IN_RETURN_POSITION")
                // In case the consumer is using experimental APIs
                .addMember("%S", "EXPERIMENTAL_API_USAGE")
                // Similarly for opt in usage
                .addMember("%S", "OPT_IN_USAGE")
                .build())
            .addTypeVariables(adapter.targetTypeVariables.map { it.withoutVariance() })
            .superclass(Types.Kotshi.namedJsonAdapter.plusParameter(adapter.targetType))
            .addSuperclassConstructorParameter(
                "%S",
                "KotshiJsonAdapter(${adapter.targetSimpleNames.joinToString(".")})"
            )
            .apply { renderSetup() }
            .addFunction(FunSpec.builder("toJson")
                .addModifiers(KModifier.OVERRIDE)
                .throws(Types.Java.ioException)
                .addParameter(writer)
                .addParameter(value)
                .apply { renderToJson(writer, value) }
                .build())
            .addFunction(FunSpec.builder("fromJson")
                .addModifiers(KModifier.OVERRIDE)
                .throws(Types.Java.ioException)
                .addParameter(reader)
                .returns(adapter.targetType.nullable())
                .apply { renderFromJson(reader) }
                .build())
            .apply(typeSpecModifier)
            .build()
        return GeneratedAdapter(
            adapter = adapter,
            fileSpec = FileSpec.builder(adapter.targetPackageName, adapter.adapterName)
                .addFileComment("Code generated by Kotshi. Do not edit.")
                .addType(typeSpec)
                .build(),
            proguardConfig = typeSpec.createProguardRule(),
            originatingElement = originatingElement,
        )
    }

    protected open fun TypeSpec.Builder.renderSetup() {}
    protected abstract fun FunSpec.Builder.renderFromJson(readerParameter: ParameterSpec)
    protected abstract fun FunSpec.Builder.renderToJson(writerParameter: ParameterSpec, valueParameter: ParameterSpec)

    protected fun jsonOptionsProperty(jsonNames: Collection<String>): PropertySpec =
        PropertySpec.builder(nameAllocator.newName("options"), jsonReaderOptions, KModifier.PRIVATE)
            .initializer(
                CodeBlock.Builder()
                    .add("%T.of(«", jsonReaderOptions)
                    .applyIf(jsonNames.size > 1) { add("\n") }
                    .applyEachIndexed(jsonNames) { index, name ->
                        if (index > 0) {
                            add(",\n")
                        }
                        add("%S", name)
                    }
                    .applyIf(jsonNames.size > 1) { add("\n") }
                    .add("»)")
                    .build())
            .build()

    companion object {
        const val moshiParameterName = "moshi"
        const val typesParameterName = "types"
    }
}

fun GeneratableJsonAdapter.createRenderer(
    createAnnotationsUsingConstructor: Boolean,
    error: (String) -> Throwable,
): AdapterRenderer =
    when (this) {
        is DataClassJsonAdapter -> DataClassAdapterRenderer(this, createAnnotationsUsingConstructor)
        is EnumJsonAdapter -> EnumAdapterRenderer(this)
        is ObjectJsonAdapter -> ObjectAdapterRenderer(this)
        is SealedClassJsonAdapter -> SealedClassAdapterRenderer(this, error)
    }