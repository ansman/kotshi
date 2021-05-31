package se.ansman.kotshi.kapt

import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.metadata.ImmutableKmClass
import com.squareup.kotlinpoet.metadata.specs.ClassInspector
import com.squareup.kotlinpoet.metadata.specs.toTypeSpec
import com.squareup.kotlinpoet.metadata.toImmutableKmClass
import javax.lang.model.element.Element

class MetadataAccessor(private val classInspector: ClassInspector) {
    private val metadataPerType = mutableMapOf<Element, ImmutableKmClass>()
    private val typeSpecPerType = mutableMapOf<Element, TypeSpec>()

    fun getMetadata(type: Element): ImmutableKmClass =
        metadataPerType.getOrPut(type) {
            type.metadata.toImmutableKmClass()
        }

    fun getTypeSpec(type: Element): TypeSpec =
        typeSpecPerType.getOrPut(type) {
            getMetadata(type).toTypeSpec(classInspector)
        }
}