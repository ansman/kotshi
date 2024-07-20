@file:Suppress("UnstableApiUsage")

package se.ansman.kotshi.kapt

import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import com.google.common.collect.SetMultimap
import com.squareup.kotlinpoet.DelicateKotlinPoetApi
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.asTypeVariableName
import kotlin.metadata.ClassKind
import kotlin.metadata.Modality
import kotlin.metadata.Visibility
import kotlin.metadata.isInner
import kotlin.metadata.kind
import kotlin.metadata.modality
import kotlin.metadata.visibility
import se.ansman.kotshi.Errors
import se.ansman.kotshi.ExperimentalKotshiApi
import se.ansman.kotshi.KotshiJsonAdapterFactory
import se.ansman.kotshi.RegisterJsonAdapter
import se.ansman.kotshi.constructors
import se.ansman.kotshi.model.GeneratedAdapter
import se.ansman.kotshi.model.GeneratedAnnotation
import se.ansman.kotshi.model.JsonAdapterFactory
import se.ansman.kotshi.model.JsonAdapterFactory.Companion.getManualAdapter
import se.ansman.kotshi.model.RegisteredAdapter
import se.ansman.kotshi.model.findKotshiConstructor
import se.ansman.kotshi.renderer.JsonAdapterFactoryRenderer
import javax.annotation.processing.Filer
import javax.annotation.processing.Messager
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeKind
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

class FactoryProcessingStep(
    override val processor: KotshiProcessor,
    private val messager: Messager,
    override val filer: Filer,
    private val types: Types,
    private val elements: Elements,
    private val generatedAnnotation: GeneratedAnnotation?,
    private val generatedAdapters: List<GeneratedAdapter<Element>>,
    private val metadataAccessor: MetadataAccessor,
) : KotshiProcessor.GeneratingProcessingStep() {

    @OptIn(ExperimentalKotshiApi::class)
    override val annotations: Set<Class<out Annotation>> = setOf(
        KotshiJsonAdapterFactory::class.java,
        RegisterJsonAdapter::class.java,
    )

    override fun process(
        elementsByAnnotation: SetMultimap<Class<out Annotation>, Element>,
        roundEnv: RoundEnvironment
    ) {
        val elements = elementsByAnnotation[KotshiJsonAdapterFactory::class.java]
            .map(MoreElements::asType)
        val manuallyRegisteredAdapters = elementsByAnnotation.getManualAdapters(elements.isNotEmpty()).toList()
        if (elements.size > 1) {
            messager.logKotshiError(
                Errors.multipleFactories(elements.map { it.qualifiedName.toString() }),
                elements.first()
            )
        } else for (element in elements) {
            try {
                generateFactory(element, manuallyRegisteredAdapters)
            } catch (e: KaptProcessingError) {
                messager.logKotshiError(e)
            }
        }
    }

    private fun generateFactory(element: TypeElement, manuallyRegisteredAdapters: List<RegisteredAdapter<Element>>) {
        val elementClassName = createClassName(metadataAccessor.getKmClass(element).name)
        val factory = JsonAdapterFactory(
            targetType = elementClassName,
            generatedAdapters = generatedAdapters,
            manuallyRegisteredAdapters = manuallyRegisteredAdapters,
        )

        val createAnnotationsUsingConstructor = metadataAccessor.getMetadata(element).supportsCreatingAnnotationsWithConstructor

        JsonAdapterFactoryRenderer(factory, createAnnotationsUsingConstructor)
            .render(generatedAnnotation) {
                addOriginatingElement(element)
                for (adapter in generatedAdapters) {
                    addOriginatingElement(adapter.originatingElement)
                }
                for (adapter in manuallyRegisteredAdapters) {
                    addOriginatingElement(adapter.originatingElement)
                }
            }
            .writeTo(filer)
    }

    @OptIn(DelicateKotlinPoetApi::class, ExperimentalKotshiApi::class)
    private fun SetMultimap<Class<out Annotation>, Element>.getManualAdapters(hasFactory: Boolean): Sequence<RegisteredAdapter<Element>> =
        this[RegisterJsonAdapter::class.java]
            .asSequence()
            .map { MoreElements.asType(it) }
            .mapNotNull { element ->
                val kmClass = metadataAccessor.getKmClassOrNull(element) ?: run {
                    messager.logKotshiError(Errors.javaClassNotSupported, element)
                    return@mapNotNull null
                }
                if (!kmClass.isObject && (!kmClass.isClass ||
                        kmClass.modality == Modality.ABSTRACT ||
                        kmClass.visibility == Visibility.LOCAL ||
                        kmClass.isInner ||
                        kmClass.modality == Modality.SEALED ||
                        kmClass.kind == ClassKind.ENUM_CLASS
                        )
                ) {
                    messager.logKotshiError(Errors.invalidRegisterAdapterType, element)
                    return@mapNotNull null
                }
                when (kmClass.visibility) {
                    Visibility.PUBLIC,
                    Visibility.INTERNAL -> {
                        // no-op
                    }

                    Visibility.PRIVATE,
                    Visibility.PROTECTED,
                    Visibility.PRIVATE_TO_THIS,
                    Visibility.LOCAL -> {
                        messager.logKotshiError(Errors.invalidRegisterAdapterVisibility, element)
                        return@mapNotNull null
                    }
                }

                if (!hasFactory) {
                    messager.logKotshiError(Errors.registeredAdapterWithoutFactory, element)
                }

                val annotation = element.getAnnotation(RegisterJsonAdapter::class.java)!!
                element.getManualAdapter(
                    logError = messager::logKotshiError,
                    getSuperClass = {
                        superclass.takeUnless { it.kind == TypeKind.NONE }?.let(MoreTypes::asTypeElement)
                    },
                    getSuperTypeName = {
                        metadataAccessor.getTypeSpecOrNull(this)?.superclass ?: superclass.asTypeName()
                    },
                    adapterClassName = createClassName(kmClass.name),
                    typeVariables = {
                        metadataAccessor.getTypeSpecOrNull(this)?.typeVariables
                            ?: typeParameters.map { it.asTypeVariableName() }
                    },
                    isObject = when (kmClass.kind) {
                        ClassKind.CLASS,
                        ClassKind.INTERFACE,
                        ClassKind.ENUM_CLASS,
                        ClassKind.ENUM_ENTRY,
                        ClassKind.ANNOTATION_CLASS -> false

                        ClassKind.OBJECT,
                        ClassKind.COMPANION_OBJECT -> true
                    },
                    isAbstract = kmClass.kind == ClassKind.INTERFACE || when (kmClass.modality) {
                        Modality.FINAL,
                        Modality.OPEN -> false

                        Modality.ABSTRACT,
                        Modality.SEALED -> true
                    },
                    priority = annotation.priority,
                    getKotshiConstructor = {
                        metadataAccessor.getTypeSpec(this).constructors().findKotshiConstructor(
                            parameters = { parameters },
                            type = { type },
                            hasDefaultValue = { defaultValue != null }
                        ) { name }
                    },
                    getJsonQualifiers = {
                        metadataAccessor.getTypeSpec(this).annotations.qualifiers(metadataAccessor)
                    }
                )
            }
}