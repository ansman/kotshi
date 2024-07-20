package se.ansman.kotshi.kapt.generators

import kotlin.metadata.KmClass
import kotlin.metadata.isData
import se.ansman.kotshi.Errors
import se.ansman.kotshi.kapt.MetadataAccessor
import se.ansman.kotshi.kapt.isObject
import se.ansman.kotshi.kapt.logKotshiError
import se.ansman.kotshi.model.GeneratableJsonAdapter
import se.ansman.kotshi.model.GlobalConfig
import se.ansman.kotshi.model.ObjectJsonAdapter
import javax.annotation.processing.Messager
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

class ObjectAdapterGenerator(
    metadataAccessor: MetadataAccessor,
    types: Types,
    elements: Elements,
    element: TypeElement,
    metadata: KmClass,
    globalConfig: GlobalConfig,
    messager: Messager,
) : AdapterGenerator(metadataAccessor, types, elements, element, metadata, globalConfig, messager) {
    init {
        require(metadata.isObject)
        if (!metadata.isData && metadataAccessor.getLanguageVersion(element) >= KotlinVersion(1, 9)) {
            messager.logKotshiError(Errors.nonDataObject, element)
        }
    }

    override fun getGeneratableJsonAdapter(): GeneratableJsonAdapter {
        return ObjectJsonAdapter(
            targetPackageName = targetClassName.packageName,
            targetSimpleNames = targetClassName.simpleNames,
            polymorphicLabels = getPolymorphicLabels()
        )
    }
}