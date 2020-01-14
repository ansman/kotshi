@file:Suppress("UnstableApiUsage")

package se.ansman.kotshi

import com.google.auto.common.MoreElements
import com.google.common.collect.SetMultimap
import com.squareup.kotlinpoet.metadata.isData
import com.squareup.kotlinpoet.metadata.isEnum
import com.squareup.kotlinpoet.metadata.isObject
import com.squareup.kotlinpoet.metadata.isSealed
import com.squareup.kotlinpoet.metadata.specs.ClassInspector
import com.squareup.kotlinpoet.metadata.toImmutableKmClass
import se.ansman.kotshi.generators.DataClassAdapterGenerator
import se.ansman.kotshi.generators.EnumAdapterGenerator
import se.ansman.kotshi.generators.GlobalConfig
import se.ansman.kotshi.generators.ObjectAdapterGenerator
import se.ansman.kotshi.generators.SealedClassAdapterGenerator
import javax.annotation.processing.Filer
import javax.annotation.processing.Messager
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.util.Elements
import javax.tools.Diagnostic

class AdaptersProcessingStep(
    override val processor: KotshiProcessor,
    private val classInspector: ClassInspector,
    private val messager: Messager,
    override val filer: Filer,
    private val adapters: MutableList<GeneratedAdapter>,
    private val elements: Elements,
    private val sourceVersion: SourceVersion
) : KotshiProcessor.GeneratingProcessingStep() {
    override val annotations: Set<Class<out Annotation>> =
        setOf(
            JsonSerializable::class.java,
            KotshiJsonAdapterFactory::class.java
        )

    override fun process(elementsByAnnotation: SetMultimap<Class<out Annotation>, Element>, roundEnv: RoundEnvironment) {
        for (element in elementsByAnnotation[Polymorphic::class.java]) {
            if (element.getAnnotation(JsonSerializable::class.java) == null) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Kotshi: Classes annotated with @Polymorphic must also be annotated with @JsonSerializable", element)
            }
        }

        val globalConfig = (elementsByAnnotation[KotshiJsonAdapterFactory::class.java]
            .firstOrNull()
            ?.getAnnotation(KotshiJsonAdapterFactory::class.java)
            ?.let(::GlobalConfig)
            ?: GlobalConfig.DEFAULT)

        for (element in elementsByAnnotation[JsonSerializable::class.java]) {
            try {
                val metadata = element.metadata.toImmutableKmClass()
                val typeElement = MoreElements.asType(element)

                val generator = when {
                    metadata.isData -> DataClassAdapterGenerator(
                        classInspector = classInspector,
                        elements = elements,
                        element = typeElement,
                        metadata = metadata,
                        globalConfig = globalConfig
                    )
                    metadata.isEnum -> EnumAdapterGenerator(
                        classInspector = classInspector,
                        elements = elements,
                        element = typeElement,
                        metadata = metadata,
                        globalConfig = globalConfig
                    )
                    metadata.isObject -> ObjectAdapterGenerator(
                        classInspector = classInspector,
                        element = typeElement,
                        metadata = metadata,
                        elements = elements,
                        globalConfig = globalConfig
                    )
                    metadata.isSealed -> SealedClassAdapterGenerator(
                        classInspector = classInspector,
                        element = typeElement,
                        metadata = metadata,
                        elements = elements,
                        globalConfig = globalConfig
                    )
                    else -> throw ProcessingError(
                        "@JsonSerializable can only be applied to enums, objects, data classes",
                        typeElement
                    )
                }

                adapters += generator.generateAdapter(sourceVersion, filer)
            } catch (e: ProcessingError) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Kotshi: ${e.message}", e.element)
            }
        }
    }
}