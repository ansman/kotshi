package se.ansman.kotshi.ksp

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName.Companion.member
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import se.ansman.kotshi.GeneratedAdapter
import se.ansman.kotshi.GlobalConfig
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.KotshiJsonAdapterFactory
import se.ansman.kotshi.KotshiUtils
import se.ansman.kotshi.Polymorphic
import se.ansman.kotshi.SerializeNulls
import se.ansman.kotshi.addControlFlow
import se.ansman.kotshi.ksp.generators.DataClassAdapterGenerator
import se.ansman.kotshi.ksp.generators.EnumAdapterGenerator
import se.ansman.kotshi.ksp.generators.ObjectAdapterGenerator
import se.ansman.kotshi.ksp.generators.SealedClassAdapterGenerator
import se.ansman.kotshi.moshiTypes
import se.ansman.kotshi.nullable
import java.lang.reflect.Type

class KotshiSymbolProcessor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        for (annotated in resolver.getSymbolsWithAnnotation(Polymorphic::class.qualifiedName!!)) {
            require(annotated is KSClassDeclaration)
            if (annotated.getAnnotation<JsonSerializable>() == null) {
                environment.logger.error("Kotshi: Classes annotated with @Polymorphic must also be annotated with @JsonSerializable", annotated)
            }
        }

        val factories = resolver.getSymbolsWithAnnotation(KotshiJsonAdapterFactory::class.qualifiedName!!)
            .toList()
        if (factories.size > 1) {
            environment.logger.error("Multiple classes found with annotations @KotshiJsonAdapterFactory", factories[0])
            return emptyList()
        }
        val factory = factories.firstOrNull()

        val globalConfig = factory
            ?.getAnnotation<KotshiJsonAdapterFactory>()
            ?.let { annotation ->
                GlobalConfig(
                    useAdaptersForPrimitives = annotation.getValue("useAdaptersForPrimitives") ?: false,
                    serializeNulls = annotation.getEnumValue("serializeNulls", SerializeNulls.DEFAULT),
                )
            }
            ?: GlobalConfig.DEFAULT

        val adapters = generateAdapters(resolver, globalConfig)
        if (factory != null) {
            try {
                generateFactory(factory as KSClassDeclaration, resolver, adapters)
            } catch (e: KspProcessingError) {
                environment.logger.error("Kotshi: ${e.message}", e.node)
            }
        }

        return emptyList()
    }

    private fun generateFactory(
        element: KSClassDeclaration,
        resolver: Resolver,
        adapters: List<GeneratedAdapter>
    ) {
        val elementClassName = element.toClassName()
        val generatedName = elementClassName.let {
            ClassName(it.packageName, "Kotshi${it.simpleNames.joinToString("_")}")
        }

        val jsonAdapterFactory = resolver.getClassDeclarationByName<JsonAdapter.Factory>()!!.asType(emptyList())
        val typeSpecBuilder = if (element.asType(emptyList())
                .isAssignableFrom(jsonAdapterFactory) && Modifier.ABSTRACT in element.modifiers
        ) {
            TypeSpec.objectBuilder(generatedName)
                .superclass(elementClassName)
        } else {
            TypeSpec.objectBuilder(generatedName)
                .addSuperinterface(jsonAdapterFactory.toTypeName())
        }

        typeSpecBuilder
            .addModifiers(KModifier.INTERNAL)
            .addOriginatingKSFile(element.containingFile!!)

        val typeParam = ParameterSpec.builder("type", Type::class)
            .build()
        val annotationsParam = ParameterSpec.builder(
            "annotations",
            Set::class.asClassName().parameterizedBy(Annotation::class.asClassName())
        )
            .build()
        val moshiParam = ParameterSpec.builder("moshi", Moshi::class)
            .build()

        val factory = typeSpecBuilder
            .addFunction(makeCreateFunction(typeParam, annotationsParam, moshiParam, adapters))
            .build()

        FileSpec.builder(generatedName.packageName, generatedName.simpleName)
            .addComment("Code generated by Kotshi. Do not edit.")
            .addType(factory)
            .build()
            .writeTo(environment.codeGenerator)
    }

    private fun generateAdapters(resolver: Resolver, globalConfig: GlobalConfig): List<GeneratedAdapter> =
        resolver.getSymbolsWithAnnotation(JsonSerializable::class.qualifiedName!!).mapNotNull { annotated ->
            require(annotated is KSClassDeclaration)
            try {
                val generator = when (annotated.classKind) {
                    ClassKind.CLASS -> {
                        when {
                            Modifier.DATA in annotated.modifiers -> {
                                DataClassAdapterGenerator(
                                    environment = environment,
                                    resolver = resolver,
                                    element = annotated,
                                    globalConfig = globalConfig
                                )
                            }
                            Modifier.SEALED in annotated.modifiers -> {
                                SealedClassAdapterGenerator(
                                    environment = environment,
                                    resolver = resolver,
                                    element = annotated,
                                    globalConfig = globalConfig
                                )
                            }
                            else -> {
                                throw KspProcessingError(
                                    "@JsonSerializable can only be applied to enums, objects, sealed classes and data classes",
                                    annotated
                                )
                            }
                        }
                    }
                    ClassKind.ENUM_CLASS -> EnumAdapterGenerator(
                        environment = environment,
                        resolver = resolver,
                        element = annotated,
                        globalConfig = globalConfig
                    )
                    ClassKind.OBJECT -> ObjectAdapterGenerator(
                        environment = environment,
                        resolver = resolver,
                        element = annotated,
                        globalConfig = globalConfig
                    )
                    else -> {
                        throw KspProcessingError(
                            "@JsonSerializable can only be applied to enums, objects, sealed classes and data classes",
                            annotated
                        )
                    }
                }

                generator.generateAdapter()
            } catch (e: KspProcessingError) {
                environment.logger.error("Kotshi: ${e.message}", e.node)
                null
            }
        }.toList()

    private fun makeCreateFunction(
        typeParam: ParameterSpec,
        annotationsParam: ParameterSpec,
        moshiParam: ParameterSpec,
        adapters: List<GeneratedAdapter>
    ): FunSpec {
        val createSpec = FunSpec.builder("create")
            .addModifiers(KModifier.OVERRIDE)
            .returns(JsonAdapter::class.asClassName().parameterizedBy(STAR).nullable())
            .addParameter(typeParam)
            .addParameter(annotationsParam)
            .addParameter(moshiParam)


        if (adapters.isEmpty()) {
            return createSpec
                .addStatement("return null")
                .build()
        }

        return createSpec
            .addStatement("if (%N.isNotEmpty()) return null", annotationsParam)
            .addCode("\n")
            .addControlFlow("return when (%T.getRawType(%N))", moshiTypes, typeParam) {
                for (adapter in adapters.sortedBy { it.className }) {
                    addCode("«%T::class.java ->\n%T", adapter.targetType, adapter.className)
                    if (adapter.typeVariables.isNotEmpty()) {
                        addCode(adapter.typeVariables.joinToString(", ", prefix = "<", postfix = ">") { "Nothing" })
                    }
                    addCode("(")
                    if (adapter.requiresMoshi) {
                        addCode("%N", moshiParam)
                    }
                    if (adapter.requiresTypes) {
                        if (adapter.requiresMoshi) {
                            addCode(", ")
                        }
                        addCode("%N.%M", typeParam, typeArgumentsOrFail)
                    }
                    addCode(")\n»")
                }
                addStatement("else -> null")
            }
            .build()
    }

    companion object {
        private val typeArgumentsOrFail = KotshiUtils::class.java.member("typeArgumentsOrFail")
    }
}