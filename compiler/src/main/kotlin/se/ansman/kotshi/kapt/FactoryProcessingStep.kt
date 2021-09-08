@file:Suppress("UnstableApiUsage")

package se.ansman.kotshi.kapt

import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import com.google.common.collect.SetMultimap
import com.squareup.kotlinpoet.DelicateKotlinPoetApi
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.asTypeVariableName
import com.squareup.kotlinpoet.metadata.isAbstract
import com.squareup.kotlinpoet.metadata.isCompanionObjectClass
import com.squareup.kotlinpoet.metadata.isInterface
import com.squareup.kotlinpoet.metadata.isObjectClass
import com.squareup.kotlinpoet.metadata.isSealed
import com.squareup.moshi.JsonAdapter
import se.ansman.kotshi.ExperimentalKotshiApi
import se.ansman.kotshi.KotshiJsonAdapterFactory
import se.ansman.kotshi.RegisterJsonAdapter
import se.ansman.kotshi.constructors
import se.ansman.kotshi.maybeAddGeneratedAnnotation
import se.ansman.kotshi.model.GeneratedAdapter
import se.ansman.kotshi.model.JsonAdapterFactory
import se.ansman.kotshi.model.JsonAdapterFactory.Companion.getManualAdapter
import se.ansman.kotshi.model.RegisteredAdapter
import se.ansman.kotshi.model.findKotshiConstructor
import se.ansman.kotshi.renderer.JsonAdapterFactoryRenderer
import javax.annotation.processing.Filer
import javax.annotation.processing.Messager
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import javax.tools.Diagnostic
import kotlin.reflect.KClass

class FactoryProcessingStep(
    override val processor: KotshiProcessor,
    private val messager: Messager,
    override val filer: Filer,
    private val types: Types,
    private val elements: Elements,
    private val sourceVersion: SourceVersion,
    private val generatedAdapters: List<GeneratedAdapter>,
    private val metadataAccessor: MetadataAccessor,
    private val createAnnotationsUsingConstructor: Boolean?,
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
        if (elements.size > 1) {
            messager.printMessage(
                Diagnostic.Kind.ERROR,
                "Multiple classes found with annotations @KotshiJsonAdapterFactory",
                elements.first()
            )
        } else for (element in elements) {
            try {
                generateFactory(MoreElements.asType(element), elementsByAnnotation)
            } catch (e: KaptProcessingError) {
                logError(e.message, e.element)
            }
        }
    }

    private fun logError(message: String, element: Element) {
        messager.printMessage(Diagnostic.Kind.ERROR, "Kotshi: $message", element)
    }

    private fun generateFactory(
        element: TypeElement,
        elementsByAnnotation: SetMultimap<Class<out Annotation>, Element>
    ) {
        val elementClassName = createClassName(metadataAccessor.getKmClass(element).name)
        val factory = JsonAdapterFactory(
            targetType = elementClassName,
            usageType = if (
                element.asType().implements(JsonAdapter.Factory::class) &&
                Modifier.ABSTRACT in element.modifiers
            ) {
                JsonAdapterFactory.UsageType.Subclass(elementClassName)
            } else {
                JsonAdapterFactory.UsageType.Standalone
            },
            generatedAdapters = generatedAdapters,
            manuallyRegisteredAdapters = elementsByAnnotation.getManualAdapters().toList(),
        )

        val createAnnotationsUsingConstructor = createAnnotationsUsingConstructor ?:
            metadataAccessor.getMetadata(element).supportsCreatingAnnotationsWithConstructor

        JsonAdapterFactoryRenderer(factory, createAnnotationsUsingConstructor)
            .render {
                maybeAddGeneratedAnnotation(elements, sourceVersion)
                addOriginatingElement(element)
            }
            .writeTo(filer)
    }

    @OptIn(DelicateKotlinPoetApi::class, ExperimentalKotshiApi::class)
    private fun SetMultimap<Class<out Annotation>, Element>.getManualAdapters(): Sequence<RegisteredAdapter> =
        this[RegisterJsonAdapter::class.java]
            .asSequence()
            .map { MoreElements.asType(it) }
            .mapNotNull { element ->
                val kmClass = metadataAccessor.getKmClassOrNull(element) ?: run {
                    logError("Only Kotlin classes can be annotated with @RegisterJsonAdapter", element)
                    return@mapNotNull null
                }
                val annotation = element.getAnnotation(RegisterJsonAdapter::class.java)!!
                element.getManualAdapter(
                    logError = ::logError,
                    getSuperClass = {
                        superclass.takeUnless { it.kind == TypeKind.NONE }?.let(MoreTypes::asTypeElement)
                    },
                    getSuperTypeName = { metadataAccessor.getTypeSpecOrNull(this)?.superclass ?: superclass.asTypeName() },
                    adapterClassName = createClassName(kmClass.name),
                    typeVariables = { metadataAccessor.getTypeSpecOrNull(this)?.typeVariables ?: typeParameters.map { it.asTypeVariableName() } },
                    isObject = kmClass.flags.isObjectClass || kmClass.flags.isCompanionObjectClass,
                    isAbstract = kmClass.flags.isInterface || kmClass.flags.isAbstract || kmClass.flags.isSealed,
                    priority = annotation.priority,
                    getKotshiConstructor = {
                        metadataAccessor.getTypeSpec(this).constructors().findKotshiConstructor(
                            parameters = { parameters },
                            type = { type },
                            hasDefaultValue = { defaultValue != null }
                        ) { name }
                    },
                    getJsonQualifiers = {
                        metadataAccessor.getTypeSpec(this).annotationSpecs.qualifiers(metadataAccessor)
                    }
                )
            }

    private fun TypeMirror.implements(someType: KClass<*>): Boolean =
        types.isSubtype(this, elements.getTypeElement(someType.java.canonicalName).asType())
}