package se.ansman.kotshi.kapt.generators

import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.metadata.isEnum
import kotlinx.metadata.KmClass
import se.ansman.kotshi.Types.Kotshi.jsonDefaultValue
import se.ansman.kotshi.kapt.MetadataAccessor
import se.ansman.kotshi.kapt.KaptProcessingError
import se.ansman.kotshi.kapt.jsonName
import se.ansman.kotshi.model.EnumJsonAdapter
import se.ansman.kotshi.model.GeneratableJsonAdapter
import se.ansman.kotshi.model.GlobalConfig
import javax.annotation.processing.Messager
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

class EnumAdapterGenerator(
    metadataAccessor: MetadataAccessor,
    types: Types,
    elements: Elements,
    element: TypeElement,
    metadata: KmClass,
    globalConfig: GlobalConfig,
    messager: Messager,
) : AdapterGenerator(metadataAccessor, types, elements, element, metadata, globalConfig, messager) {
    init {
        require(metadata.isEnum)
    }

    override fun getGenerableAdapter(): GeneratableJsonAdapter =
        EnumJsonAdapter(
            targetPackageName = targetClassName.packageName,
            targetSimpleNames = targetClassName.simpleNames,
            entries = targetTypeSpec.enumConstants.entries.map { it.toEntry() },
            fallback = targetTypeSpec.enumConstants
                .entries
                .filter { (_, constant) ->
                    constant.annotationSpecs.any { it.typeName == jsonDefaultValue }
                }
                .takeIf { it.isNotEmpty() }
                ?.let { defaultValues ->
                    defaultValues.singleOrNull()
                        ?.toEntry()
                        ?: throw KaptProcessingError(
                            "Only one enum entry can be annotated with @JsonDefaultValue",
                            targetElement
                        )
                }
        )

    private fun Map.Entry<String, TypeSpec>.toEntry(): EnumJsonAdapter.Entry =
        EnumJsonAdapter.Entry(
            name = key,
            serializedName = value.annotationSpecs.jsonName() ?: key
        )
}