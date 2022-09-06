package se.ansman.kotshi.ksp

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.toTypeVariableName
import com.squareup.kotlinpoet.ksp.writeTo
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonQualifier
import se.ansman.kotshi.ExperimentalKotshiApi
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.KotshiJsonAdapterFactory
import se.ansman.kotshi.Polymorphic
import se.ansman.kotshi.RegisterJsonAdapter
import se.ansman.kotshi.SerializeNulls
import se.ansman.kotshi.ksp.generators.DataClassAdapterGenerator
import se.ansman.kotshi.ksp.generators.EnumAdapterGenerator
import se.ansman.kotshi.ksp.generators.ObjectAdapterGenerator
import se.ansman.kotshi.ksp.generators.SealedClassAdapterGenerator
import se.ansman.kotshi.model.GeneratedAdapter
import se.ansman.kotshi.model.GlobalConfig
import se.ansman.kotshi.model.JsonAdapterFactory
import se.ansman.kotshi.model.JsonAdapterFactory.Companion.getManualAdapter
import se.ansman.kotshi.model.findKotshiConstructor
import se.ansman.kotshi.renderer.JsonAdapterFactoryRenderer

class KotshiSymbolProcessor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {
    private val createAnnotationsUsingConstructor = environment.options["kotshi.createAnnotationsUsingConstructor"]
        ?.toBooleanStrict()
        ?: environment.kotlinVersion.isAtLeast(1, 6)

    private fun logError(message: String, node: KSNode) {
        environment.logger.error("Kotshi: $message", node)
    }

    @OptIn(ExperimentalKotshiApi::class, KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        for (annotated in resolver.getSymbolsWithAnnotation(Polymorphic::class.qualifiedName!!)) {
            require(annotated is KSClassDeclaration)
            if (annotated.getAnnotation<JsonSerializable>() == null) {
                logError("Classes annotated with @Polymorphic must also be annotated with @JsonSerializable", annotated)
            }
        }

        val factories = resolver.getSymbolsWithAnnotation(KotshiJsonAdapterFactory::class.qualifiedName!!)
            .toList()
        if (factories.size > 1) {
            logError("Multiple classes found with annotations @KotshiJsonAdapterFactory", factories[0])
            return emptyList()
        }
        val targetFactory = factories.firstOrNull()

        val globalConfig = targetFactory
            ?.getAnnotation<KotshiJsonAdapterFactory>()
            ?.let { annotation ->
                GlobalConfig(
                    useAdaptersForPrimitives = annotation.getValue("useAdaptersForPrimitives") ?: false,
                    serializeNulls = annotation.getEnumValue("serializeNulls", SerializeNulls.DEFAULT),
                )
            }
            ?: GlobalConfig.DEFAULT


        val manualAdapters = resolver.getSymbolsWithAnnotation(RegisterJsonAdapter::class.qualifiedName!!)
            .mapNotNull {
                if (it !is KSClassDeclaration) {
                    logError("Only types can be annotated with @RegisterJsonAdapter", it)
                    null
                } else when (it.classKind) {
                    ClassKind.CLASS,
                    ClassKind.OBJECT -> it

                    ClassKind.INTERFACE,
                    ClassKind.ENUM_CLASS,
                    ClassKind.ENUM_ENTRY,
                    ClassKind.ANNOTATION_CLASS -> {
                        logError("Only classes and objects can be annotated with @RegisterJsonAdapter", it)
                        null
                    }
                }
            }
            .mapNotNull { adapter ->
                val annotation = adapter.getAnnotation<RegisterJsonAdapter>()!!
                adapter.getManualAdapter(
                    logError = ::logError,
                    getSuperClass = { superClass()?.declaration as KSClassDeclaration },
                    getSuperTypeName = { superClass()?.toTypeName(toTypeParameterResolver()) },
                    adapterClassName = adapter.asClassName(),
                    typeVariables = { typeParameters.map { it.toTypeVariableName(toTypeParameterResolver()) } },
                    isObject = adapter.isCompanionObject || adapter.classKind == ClassKind.OBJECT,
                    isAbstract = adapter.isAbstract(),
                    priority = annotation.getValueOrDefault("priority") { 0 },
                    getKotshiConstructor = {
                        val typeResolver = toTypeParameterResolver()

                        getAllConstructors().findKotshiConstructor(
                            parameters = { parameters },
                            type = { type.toTypeName(typeResolver) },
                            hasDefaultValue = { hasDefault }
                        ) { requireNotNull(name).asString() }
                    },
                    getJsonQualifiers = {
                        annotations
                            .filter {
                                it.annotationType.resolve().declaration.isAnnotationPresent(JsonQualifier::class)
                            }
                            .map { annotation ->
                                annotation.toAnnotationModel()
                            }
                            .toSet()
                    }
                )
            }
            .toList()

        if (targetFactory != null) {
            try {
                if (targetFactory !is KSClassDeclaration || targetFactory.classKind == ClassKind.CLASS && Modifier.ABSTRACT !in targetFactory.modifiers) {
                    throw KspProcessingError(
                        "@KotshiJsonAdapterFactory must be applied to an object or abstract class",
                        targetFactory
                    )
                }

                val jsonAdapterFactoryType = resolver.getClassDeclarationByName<JsonAdapter.Factory>()!!
                    .asType(emptyList())
                val factory = JsonAdapterFactory(
                    targetType = targetFactory.toClassName(),
                    usageType = if (
                        targetFactory.asType(emptyList())
                            .isAssignableFrom(jsonAdapterFactoryType) && Modifier.ABSTRACT in targetFactory.modifiers
                    ) {
                        JsonAdapterFactory.UsageType.Subclass(targetFactory.toClassName())
                    } else {
                        JsonAdapterFactory.UsageType.Standalone
                    },
                    generatedAdapters = generateAdapters(resolver, globalConfig),
                    manuallyRegisteredAdapters = manualAdapters,
                )
                JsonAdapterFactoryRenderer(factory, createAnnotationsUsingConstructor)
                    .render { addOriginatingKSFile(targetFactory.containingFile!!) }
                    .writeTo(environment.codeGenerator, aggregating = true)
            } catch (e: KspProcessingError) {
                logError(e.message, e.node)
            } catch (e: Exception) {
                logError("Failed to analyze class:", targetFactory)
                environment.logger.exception(e)
            }
        }

        return emptyList()
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
                                    element = annotated,
                                    globalConfig = globalConfig,
                                )
                            }
                            Modifier.SEALED in annotated.modifiers -> {
                                SealedClassAdapterGenerator(
                                    environment = environment,
                                    targetElement = annotated,
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
                        element = annotated,
                        globalConfig = globalConfig
                    )
                    ClassKind.OBJECT -> ObjectAdapterGenerator(
                        environment = environment,
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

                generator.generateAdapter(createAnnotationsUsingConstructor)
            } catch (e: KspProcessingError) {
                logError(e.message, e.node)
                null
            } catch (e: Exception) {
                logError("Failed to analyze class:", annotated)
                environment.logger.exception(e)
                null
            }
        }.toList()
}