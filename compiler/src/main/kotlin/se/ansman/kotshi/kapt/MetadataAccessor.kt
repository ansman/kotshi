package se.ansman.kotshi.kapt

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.DelicateKotlinPoetApi
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.metadata.ImmutableKmClass
import com.squareup.kotlinpoet.metadata.specs.ClassInspector
import com.squareup.kotlinpoet.metadata.specs.internal.ClassInspectorUtil
import com.squareup.kotlinpoet.metadata.specs.toTypeSpec
import com.squareup.kotlinpoet.metadata.toImmutableKmClass
import com.squareup.kotlinpoet.tag
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement

class MetadataAccessor(private val classInspector: ClassInspector) {
    private val metadataPerType = mutableMapOf<ClassName, ImmutableKmClass>()
    private val typeSpecPerKmClass = mutableMapOf<ImmutableKmClass, TypeSpec>()

    fun getMetadata(type: Element): ImmutableKmClass =
        @OptIn(DelicateKotlinPoetApi::class) // OK because we are using the class name for comparisson
        metadataPerType.getOrPut((type as TypeElement).asClassName()) {
            type.getAnnotation(Metadata::class.java)
                ?.toImmutableKmClass()
                ?: throw KaptProcessingError("Class must be written in Kotlin", type)
        }

    fun getTypeSpec(type: Element): TypeSpec = getTypeSpec(getMetadata(type))

    fun getTypeSpec(metadata: ImmutableKmClass): TypeSpec =
        typeSpecPerKmClass.getOrPut(metadata) {
            metadata.toTypeSpec(classInspector)
                .toBuilder()
                .tag(ClassInspectorUtil.createClassName(metadata.name))
                .build()
        }
}