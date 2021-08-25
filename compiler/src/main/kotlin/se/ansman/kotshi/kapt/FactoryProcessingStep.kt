@file:Suppress("UnstableApiUsage")

package se.ansman.kotshi.kapt

import com.google.auto.common.MoreElements
import com.google.common.collect.SetMultimap
import com.squareup.moshi.JsonAdapter
import se.ansman.kotshi.KotshiJsonAdapterFactory
import se.ansman.kotshi.maybeAddGeneratedAnnotation
import se.ansman.kotshi.model.GeneratedAdapter
import se.ansman.kotshi.model.JsonAdapterFactory
import se.ansman.kotshi.renderer.JsonAdapterFactoryRenderer
import javax.annotation.processing.Filer
import javax.annotation.processing.Messager
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
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
    private val adapters: List<GeneratedAdapter>,
    private val metadataAccessor: MetadataAccessor,
) : KotshiProcessor.GeneratingProcessingStep() {

    private fun TypeMirror.implements(someType: KClass<*>): Boolean =
        types.isSubtype(this, elements.getTypeElement(someType.java.canonicalName).asType())

    override val annotations: Set<Class<out Annotation>> = setOf(KotshiJsonAdapterFactory::class.java)

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
                generateFactory(MoreElements.asType(element))
            } catch (e: KaptProcessingError) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Kotshi: ${e.message}", e.element)
            }
        }
    }

    private fun generateFactory(element: TypeElement) {
        val metadata = metadataAccessor.getKmClass(element)
        val elementClassName = createClassName(metadata.name)

        val factory = JsonAdapterFactory(
            targetType = elementClassName,
            usageType = if (element.asType().implements(JsonAdapter.Factory::class) && Modifier.ABSTRACT in element.modifiers) {
                JsonAdapterFactory.UsageType.Subclass(elementClassName)
            } else {
                JsonAdapterFactory.UsageType.Standalone
            },
            adapters = adapters
        )

        JsonAdapterFactoryRenderer(factory)
            .render {
                maybeAddGeneratedAnnotation(elements, sourceVersion)
                addOriginatingElement(element)
            }
            .writeTo(filer)
    }
}