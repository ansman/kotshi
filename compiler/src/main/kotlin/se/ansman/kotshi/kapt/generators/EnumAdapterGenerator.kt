package se.ansman.kotshi.kapt.generators

import com.squareup.kotlinpoet.TypeSpec
import kotlin.metadata.KmClass
import se.ansman.kotshi.Errors
import se.ansman.kotshi.Types.Kotshi.jsonDefaultValue
import se.ansman.kotshi.kapt.KaptProcessingError
import se.ansman.kotshi.kapt.MetadataAccessor
import se.ansman.kotshi.kapt.isEnum
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

    override fun getGeneratableJsonAdapter(): GeneratableJsonAdapter =
        EnumJsonAdapter(
            targetPackageName = targetClassName.packageName,
            targetSimpleNames = targetClassName.simpleNames,
            entries = targetTypeSpec.enumConstants.entries.map { it.toEntry() },
            fallback = targetTypeSpec.enumConstants
                .entries
                .filter { (_, constant) ->
                    constant.annotations.any { it.typeName == jsonDefaultValue }
                }
                .let { defaultValues ->
                    when (defaultValues.size) {
                        0 -> null
                        1 -> defaultValues[0].toEntry()
                        else -> throw KaptProcessingError(Errors.multipleJsonDefaultValueInEnum, targetElement)
                    }
                }
        )

    private fun Map.Entry<String, TypeSpec>.toEntry(): EnumJsonAdapter.Entry =
        EnumJsonAdapter.Entry(
            name = key,
            serializedName = value.annotations.jsonName() ?: key
        )
}