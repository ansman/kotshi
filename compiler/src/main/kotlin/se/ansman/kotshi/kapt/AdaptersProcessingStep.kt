@file:Suppress("UnstableApiUsage")

package se.ansman.kotshi.kapt

import com.google.auto.common.MoreElements
import com.google.common.collect.SetMultimap
import kotlin.metadata.Modality
import kotlin.metadata.isData
import kotlin.metadata.modality
import se.ansman.kotshi.Errors
import se.ansman.kotshi.JsonDefaultValue
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.KotshiJsonAdapterFactory
import se.ansman.kotshi.Polymorphic
import se.ansman.kotshi.kapt.generators.DataClassAdapterGenerator
import se.ansman.kotshi.kapt.generators.EnumAdapterGenerator
import se.ansman.kotshi.kapt.generators.ObjectAdapterGenerator
import se.ansman.kotshi.kapt.generators.SealedClassAdapterGenerator
import se.ansman.kotshi.model.GeneratedAdapter
import se.ansman.kotshi.model.GeneratedAnnotation
import se.ansman.kotshi.model.GlobalConfig
import javax.annotation.processing.Filer
import javax.annotation.processing.Messager
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.Modifier
import javax.lang.model.element.NestingKind
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

class AdaptersProcessingStep(
    override val processor: KotshiProcessor,
    private val metadataAccessor: MetadataAccessor,
    private val messager: Messager,
    override val filer: Filer,
    private val adapters: MutableList<GeneratedAdapter<Element>>,
    private val types: Types,
    private val elements: Elements,
    private val generatedAnnotation: GeneratedAnnotation?,
) : KotshiProcessor.GeneratingProcessingStep() {
    override val annotations: Set<Class<out Annotation>> =
        setOf(
            Polymorphic::class.java,
            JsonDefaultValue::class.java,
            JsonSerializable::class.java,
            KotshiJsonAdapterFactory::class.java
        )

    override fun process(
        elementsByAnnotation: SetMultimap<Class<out Annotation>, Element>,
        roundEnv: RoundEnvironment
    ) {
        for (element in elementsByAnnotation[Polymorphic::class.java]) {
            if (element.getAnnotation(JsonSerializable::class.java) == null) {
                messager.logKotshiError(Errors.polymorphicClassMustHaveJsonSerializable, element)
            }
        }

        for (element in elementsByAnnotation[JsonDefaultValue::class.java]) {
            when (element.kind) {
                ElementKind.ENUM_CONSTANT -> {
                    // Ok
                }
                ElementKind.CLASS -> {
                    val metadata = metadataAccessor.getKmClass(element)
                    if (!metadata.isObject && !metadata.isData) {
                        messager.logKotshiError(Errors.jsonDefaultValueAppliedToInvalidType, element)
                    }

                }
                else -> messager.logKotshiError(Errors.jsonDefaultValueAppliedToInvalidType, element)
            }
        }

        val globalConfig = (elementsByAnnotation[KotshiJsonAdapterFactory::class.java]
            .firstOrNull()
            ?.getAnnotation(KotshiJsonAdapterFactory::class.java)
            ?.let(::GlobalConfig)
            ?: GlobalConfig.DEFAULT)

        for (element in elementsByAnnotation[JsonSerializable::class.java]) {
            try {
                val metadata = metadataAccessor.getKmClass(element)
                val typeElement = MoreElements.asType(element)
                when (typeElement.nestingKind!!) {
                    NestingKind.MEMBER ->
                        if (Modifier.STATIC !in typeElement.modifiers) {
                            throw KaptProcessingError(Errors.dataClassCannotBeInner, typeElement)
                        }

                    NestingKind.TOP_LEVEL -> {
                        // Allowed
                    }

                    NestingKind.ANONYMOUS,
                    NestingKind.LOCAL -> throw KaptProcessingError(Errors.dataClassCannotBeLocal, typeElement)
                }

                val generator = when {
                    metadata.isObject -> ObjectAdapterGenerator(
                        metadataAccessor = metadataAccessor,
                        types = types,
                        element = typeElement,
                        metadata = metadata,
                        elements = elements,
                        globalConfig = globalConfig,
                        messager = messager,
                    )

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

                    metadata.modality == Modality.SEALED -> SealedClassAdapterGenerator(
                        metadataAccessor = metadataAccessor,
                        types = types,
                        element = typeElement,
                        metadata = metadata,
                        elements = elements,
                        globalConfig = globalConfig,
                        messager = messager
                    )

                    else -> throw KaptProcessingError(Errors.unsupportedSerializableType, typeElement)
                }

                adapters += generator.generateAdapter(
                    generatedAnnotation = generatedAnnotation,
                    filer = filer,
                )
            } catch (e: KaptProcessingError) {
                messager.logKotshiError(e)
            }
        }
    }
}