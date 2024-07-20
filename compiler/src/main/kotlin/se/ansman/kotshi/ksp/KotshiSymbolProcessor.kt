package se.ansman.kotshi.ksp

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Origin
import com.google.devtools.ksp.symbol.Visibility
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.toTypeVariableName
import com.squareup.kotlinpoet.ksp.writeTo
import com.squareup.moshi.JsonQualifier
import se.ansman.kotshi.Errors
import se.ansman.kotshi.Errors.javaClassNotSupported
import se.ansman.kotshi.Errors.polymorphicClassMustHaveJsonSerializable
import se.ansman.kotshi.Errors.unsupportedFactoryType
import se.ansman.kotshi.ExperimentalKotshiApi
import se.ansman.kotshi.JsonDefaultValue
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.KotshiJsonAdapterFactory
import se.ansman.kotshi.Options
import se.ansman.kotshi.Polymorphic
import se.ansman.kotshi.RegisterJsonAdapter
import se.ansman.kotshi.SerializeNulls
import se.ansman.kotshi.ksp.generators.DataClassAdapterGenerator
import se.ansman.kotshi.ksp.generators.EnumAdapterGenerator
import se.ansman.kotshi.ksp.generators.ObjectAdapterGenerator
import se.ansman.kotshi.ksp.generators.SealedClassAdapterGenerator
import se.ansman.kotshi.model.GeneratedAdapter
import se.ansman.kotshi.model.GeneratedAnnotation
import se.ansman.kotshi.model.GlobalConfig
import se.ansman.kotshi.model.JsonAdapterFactory
import se.ansman.kotshi.model.JsonAdapterFactory.Companion.getManualAdapter
import se.ansman.kotshi.model.findKotshiConstructor
import se.ansman.kotshi.renderer.JsonAdapterFactoryRenderer

class KotshiSymbolProcessor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {
    private val createAnnotationsUsingConstructor = environment.kotlinVersion.isAtLeast(1, 6)

    private val generatedAnnotation = environment.options[Options.generatedAnnotation]
        ?.let { name ->
            Options.possibleGeneratedAnnotations[name] ?: run {
                environment.logger.logKotshiError(Errors.invalidGeneratedAnnotation(name), node = null)
                null
            }
        }
        ?.let { GeneratedAnnotation(it, KotshiSymbolProcessor::class.asClassName()) }

    @OptIn(ExperimentalKotshiApi::class, KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        for (annotated in resolver.getSymbolsWithAnnotation(Polymorphic::class.qualifiedName!!)) {
            if (annotated.origin == Origin.JAVA || annotated.origin == Origin.JAVA_LIB) {
                environment.logger.logKotshiError(javaClassNotSupported, annotated)
                continue
            }
            require(annotated is KSClassDeclaration)
            if (annotated.getAnnotation<JsonSerializable>() == null) {
                environment.logger.logKotshiError(polymorphicClassMustHaveJsonSerializable, annotated)
            }
        }

        for (annotated in resolver.getSymbolsWithAnnotation(JsonDefaultValue::class.qualifiedName!!)) {
            if (annotated.origin == Origin.JAVA || annotated.origin == Origin.JAVA_LIB) {
                environment.logger.logKotshiError(javaClassNotSupported, annotated)
                continue
            }
            when (annotated) {
                is KSClassDeclaration -> {
                    when (annotated.classKind) {
                        ClassKind.ENUM_ENTRY,
                        ClassKind.OBJECT -> {
                            // OK
                        }

                        ClassKind.CLASS ->
                            if (Modifier.DATA !in annotated.modifiers) {
                                environment.logger.logKotshiError(
                                    Errors.jsonDefaultValueAppliedToInvalidType,
                                    annotated
                                )
                            }

                        else -> environment.logger.logKotshiError(
                            Errors.jsonDefaultValueAppliedToInvalidType,
                            annotated
                        )
                    }
                }

                else -> environment.logger.logKotshiError(Errors.jsonDefaultValueAppliedToInvalidType, annotated)
            }
        }

        val factories = resolver.getSymbolsWithAnnotation(KotshiJsonAdapterFactory::class.qualifiedName!!)
            .mapNotNull {
                if (it.origin == Origin.JAVA || it.origin == Origin.JAVA_LIB) {
                    environment.logger.logKotshiError(javaClassNotSupported, it)
                    return@mapNotNull null
                }
                if (it !is KSClassDeclaration) {
                    environment.logger.logKotshiError(unsupportedFactoryType, it)
                    return@mapNotNull null
                }

                val isValid = it.classKind == ClassKind.CLASS && Modifier.ABSTRACT in it.modifiers ||
                    it.classKind == ClassKind.OBJECT ||
                    it.classKind == ClassKind.INTERFACE
                if (isValid) {
                    it
                } else {
                    environment.logger.logKotshiError(unsupportedFactoryType, it)
                    null
                }
            }
            .toList()
        if (factories.size > 1) {
            environment.logger.logKotshiError(
                Errors.multipleFactories(factories.map { it.qualifiedName?.asString() ?: it.simpleName.asString() }),
                factories[0]
            )
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
            .onEach {
                if (targetFactory == null) {
                    environment.logger.logKotshiError(Errors.registeredAdapterWithoutFactory, it)
                }
            }
            .mapNotNull {
                if (it !is KSClassDeclaration) {
                    environment.logger.logKotshiError(Errors.invalidRegisterAdapterType, it)
                    null
                } else when (it.classKind) {
                    ClassKind.CLASS,
                    ClassKind.OBJECT -> {
                        if (it.isAbstract()) {
                            environment.logger.logKotshiError(Errors.invalidRegisterAdapterType, it)
                            null
                        } else {
                            it
                        }
                    }

                    ClassKind.INTERFACE,
                    ClassKind.ENUM_CLASS,
                    ClassKind.ENUM_ENTRY,
                    ClassKind.ANNOTATION_CLASS -> {
                        environment.logger.logKotshiError(Errors.invalidRegisterAdapterType, it)
                        null
                    }
                }
            }
            .onEach {
                when (it.getVisibility()) {
                    Visibility.PUBLIC,
                    Visibility.INTERNAL -> {
                        // Ok
                    }

                    else -> {
                        environment.logger.logKotshiError(Errors.invalidRegisterAdapterVisibility, it)
                    }
                }
            }
            .mapNotNull { adapter ->
                val annotation = adapter.getAnnotation<RegisterJsonAdapter>()!!
                adapter.getManualAdapter(
                    logError = environment.logger::logKotshiError,
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

        val generatedAdapters = generateAdapters(resolver, globalConfig)

        if (targetFactory != null) {
            try {
                val factory = JsonAdapterFactory(
                    targetType = targetFactory.toClassName(),
                    generatedAdapters = generatedAdapters,
                    manuallyRegisteredAdapters = manualAdapters,
                )
                JsonAdapterFactoryRenderer(factory, createAnnotationsUsingConstructor)
                    .render(generatedAnnotation) {
                        addOriginatingKSFile(targetFactory.containingFile!!)
                        for (adapter in generatedAdapters) {
                            addOriginatingKSFile(adapter.originatingElement)
                        }
                        for (adapter in manualAdapters) {
                            addOriginatingKSFile(adapter.originatingElement.containingFile!!)
                        }
                    }
                    .writeTo(environment.codeGenerator, aggregating = true)
            } catch (e: KspProcessingError) {
                environment.logger.logKotshiError(e)
            } catch (e: Exception) {
                environment.logger.logKotshiError("Failed to analyze class:", targetFactory)
                environment.logger.exception(e)
            }
        }

        return emptyList()
    }

    private fun generateAdapters(resolver: Resolver, globalConfig: GlobalConfig): List<GeneratedAdapter<KSFile>> =
        resolver.getSymbolsWithAnnotation(JsonSerializable::class.qualifiedName!!).mapNotNull { annotated ->
            if (annotated.origin == Origin.JAVA || annotated.origin == Origin.JAVA_LIB) {
                environment.logger.logKotshiError(javaClassNotSupported, annotated)
                return@mapNotNull null
            }
            if (annotated !is KSClassDeclaration) {
                if (annotated is KSFunctionDeclaration && annotated.origin == Origin.SYNTHETIC) {
                    // This is a workaround for https://github.com/google/ksp/issues/1996
                    return@mapNotNull null
                }
                environment.logger.logKotshiError(Errors.unsupportedSerializableType, annotated)
                return@mapNotNull null
            }
            try {
                val generator = when (annotated.classKind) {
                    ClassKind.CLASS -> {
                        when {
                            Modifier.DATA in annotated.modifiers -> {
                                DataClassAdapterGenerator(
                                    environment = environment,
                                    element = annotated,
                                    globalConfig = globalConfig,
                                    resolver = resolver,
                                )
                            }

                            Modifier.SEALED in annotated.modifiers -> {
                                SealedClassAdapterGenerator(
                                    environment = environment,
                                    targetElement = annotated,
                                    globalConfig = globalConfig,
                                    resolver = resolver,
                                )
                            }

                            else -> throw KspProcessingError(Errors.unsupportedSerializableType, annotated)
                        }
                    }

                    ClassKind.ENUM_CLASS -> EnumAdapterGenerator(
                        environment = environment,
                        element = annotated,
                        globalConfig = globalConfig,
                        resolver = resolver,
                    )

                    ClassKind.OBJECT -> ObjectAdapterGenerator(
                        environment = environment,
                        element = annotated,
                        globalConfig = globalConfig,
                        resolver = resolver,
                    )

                    else -> throw KspProcessingError(Errors.unsupportedSerializableType, annotated)
                }

                generator.generateAdapter(
                    createAnnotationsUsingConstructor = createAnnotationsUsingConstructor,
                    generatedAnnotation = generatedAnnotation,
                )
            } catch (e: KspProcessingError) {
                environment.logger.logKotshiError(e)
                null
            } catch (e: Exception) {
                environment.logger.logKotshiError("Failed to analyze class:", annotated)
                environment.logger.exception(e)
                null
            }
        }.toList()
}