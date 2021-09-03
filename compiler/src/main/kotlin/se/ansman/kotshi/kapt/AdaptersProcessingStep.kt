@file:Suppress("UnstableApiUsage")

package se.ansman.kotshi.kapt

import com.google.auto.common.MoreElements
import com.google.common.collect.SetMultimap
import com.squareup.kotlinpoet.metadata.isData
import com.squareup.kotlinpoet.metadata.isEnum
import com.squareup.kotlinpoet.metadata.isObject
import com.squareup.kotlinpoet.metadata.isSealed
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.KotshiJsonAdapterFactory
import se.ansman.kotshi.Polymorphic
import se.ansman.kotshi.kapt.generators.DataClassAdapterGenerator
import se.ansman.kotshi.kapt.generators.EnumAdapterGenerator
import se.ansman.kotshi.kapt.generators.ObjectAdapterGenerator
import se.ansman.kotshi.kapt.generators.SealedClassAdapterGenerator
import se.ansman.kotshi.model.GeneratedAdapter
import se.ansman.kotshi.model.GlobalConfig
import javax.annotation.processing.Filer
import javax.annotation.processing.Messager
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import javax.tools.Diagnostic

class AdaptersProcessingStep(
    override val processor: KotshiProcessor,
    private val metadataAccessor: MetadataAccessor,
    private val messager: Messager,
    override val filer: Filer,
    private val adapters: MutableList<GeneratedAdapter>,
    private val types: Types,
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
                val metadata = metadataAccessor.getMetadata(element)
                val typeElement = MoreElements.asType(element)

                val generator = when {
                    metadata.isData -> DataClassAdapterGenerator(
                        metadataAccessor = metadataAccessor,
                        types = types,
                        elements = elements,
                        element = typeElement,
                        metadata = metadata,
                        globalConfig = globalConfig,
                        messager = messager,
                    )
                    metadata.isEnum -> EnumAdapterGenerator(
                        metadataAccessor = metadataAccessor,
                        types = types,
                        elements = elements,
                        element = typeElement,
                        metadata = metadata,
                        globalConfig = globalConfig,
                        messager = messager
                    )
                    metadata.isObject -> ObjectAdapterGenerator(
                        metadataAccessor = metadataAccessor,
                        types = types,
                        element = typeElement,
                        metadata = metadata,
                        elements = elements,
                        globalConfig = globalConfig,
                        messager = messager
                    )
                    metadata.isSealed -> SealedClassAdapterGenerator(
                        metadataAccessor = metadataAccessor,
                        types = types,
                        element = typeElement,
                        metadata = metadata,
                        elements = elements,
                        globalConfig = globalConfig,
                        messager = messager
                    )
                    else -> throw KaptProcessingError(
                        "@JsonSerializable can only be applied to enums, objects, sealed classes and data classes",
                        typeElement
                    )
                }

                adapters += generator.generateAdapter(sourceVersion, filer)
            } catch (e: KaptProcessingError) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Kotshi: ${e.message}", e.element)
            }
        }
    }
}