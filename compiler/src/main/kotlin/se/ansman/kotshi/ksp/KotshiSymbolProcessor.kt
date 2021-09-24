package se.ansman.kotshi.ksp

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import com.squareup.moshi.JsonAdapter
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.KotshiJsonAdapterFactory
import se.ansman.kotshi.Polymorphic
import se.ansman.kotshi.SerializeNulls
import se.ansman.kotshi.ksp.generators.DataClassAdapterGenerator
import se.ansman.kotshi.ksp.generators.EnumAdapterGenerator
import se.ansman.kotshi.ksp.generators.ObjectAdapterGenerator
import se.ansman.kotshi.ksp.generators.SealedClassAdapterGenerator
import se.ansman.kotshi.model.GeneratedAdapter
import se.ansman.kotshi.model.GlobalConfig
import se.ansman.kotshi.model.JsonAdapterFactory
import se.ansman.kotshi.renderer.JsonAdapterFactoryRenderer

class KotshiSymbolProcessor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        for (annotated in resolver.getSymbolsWithAnnotation(Polymorphic::class.qualifiedName!!)) {
            require(annotated is KSClassDeclaration)
            if (annotated.getAnnotation<JsonSerializable>() == null) {
                environment.logger.error(
                    "Kotshi: Classes annotated with @Polymorphic must also be annotated with @JsonSerializable",
                    annotated
                )
            }
        }

        val factories = resolver.getSymbolsWithAnnotation(KotshiJsonAdapterFactory::class.qualifiedName!!)
            .toList()
        if (factories.size > 1) {
            environment.logger.error("Multiple classes found with annotations @KotshiJsonAdapterFactory", factories[0])
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


        val adapters = generateAdapters(resolver, globalConfig)
        if (targetFactory != null) {
            try {
                if (targetFactory !is KSClassDeclaration || (targetFactory.classKind != ClassKind.OBJECT && targetFactory.classKind != ClassKind.CLASS)) {
                    throw KspProcessingError("@KotshiJsonAdapterFactory must be applied to an object or abstract class", targetFactory)
                }

                if (targetFactory.classKind == ClassKind.CLASS && Modifier.ABSTRACT !in targetFactory.modifiers) {
                    throw KspProcessingError("@KotshiJsonAdapterFactory must be applied to an object or abstract class", targetFactory)
                }

                val jsonAdapterFactoryType = resolver.getClassDeclarationByName<JsonAdapter.Factory>()!!.asType(emptyList())
                val factory = JsonAdapterFactory(
                    targetType = targetFactory.toClassName(),
                    usageType = if (targetFactory.asType(emptyList())
                            .isAssignableFrom(jsonAdapterFactoryType) && Modifier.ABSTRACT in targetFactory.modifiers
                    ) {
                        JsonAdapterFactory.UsageType.Subclass(targetFactory.toClassName())
                    } else {
                        JsonAdapterFactory.UsageType.Standalone
                    },
                    adapters = adapters
                )
                JsonAdapterFactoryRenderer(factory)
                    .render { addOriginatingKSFile(targetFactory.containingFile!!) }
                    .writeTo(environment.codeGenerator, aggregating = true)
            } catch (e: KspProcessingError) {
                environment.logger.error("Kotshi: ${e.message}", e.node)
            } catch (e: Exception) {
                environment.logger.error("Kotshi: failed to analyze class:", targetFactory)
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
                                    resolver = resolver,
                                    element = annotated,
                                    globalConfig = globalConfig
                                )
                            }
                            Modifier.SEALED in annotated.modifiers -> {
                                SealedClassAdapterGenerator(
                                    environment = environment,
                                    resolver = resolver,
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
            } catch (e: Exception) {
                environment.logger.error("Kotshi: failed to analyze class:", annotated)
                environment.logger.exception(e)
                null
            }
        }.toList()
}