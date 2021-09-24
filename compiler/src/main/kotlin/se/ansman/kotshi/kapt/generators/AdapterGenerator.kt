@file:OptIn(InternalKotshiApi::class)

package se.ansman.kotshi.kapt.generators

import com.google.auto.common.MoreElements
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.metadata.isInner
import com.squareup.kotlinpoet.metadata.isInternal
import com.squareup.kotlinpoet.metadata.isLocal
import com.squareup.kotlinpoet.metadata.isPublic
import com.squareup.kotlinpoet.tag
import kotlinx.metadata.KmClass
import se.ansman.kotshi.InternalKotshiApi
import se.ansman.kotshi.Polymorphic
import se.ansman.kotshi.PolymorphicLabel
import se.ansman.kotshi.getPolymorphicLabels
import se.ansman.kotshi.kapt.KaptProcessingError
import se.ansman.kotshi.kapt.MetadataAccessor
import se.ansman.kotshi.maybeAddGeneratedAnnotation
import se.ansman.kotshi.model.GeneratableJsonAdapter
import se.ansman.kotshi.model.GeneratedAdapter
import se.ansman.kotshi.model.GlobalConfig
import se.ansman.kotshi.renderer.createRenderer
import javax.annotation.processing.Filer
import javax.annotation.processing.Messager
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeKind
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

@Suppress("UnstableApiUsage")
abstract class AdapterGenerator(
    protected val metadataAccessor: MetadataAccessor,
    protected val types: Types,
    protected val elements: Elements,
    protected val targetElement: TypeElement,
    protected val metadata: KmClass,
    protected val globalConfig: GlobalConfig,
    protected val messager: Messager,
) {
    protected val targetTypeSpec = metadataAccessor.getTypeSpec(metadata)

    protected val targetClassName: ClassName = targetTypeSpec.tag()!!

    protected fun TypeSpec.getTypeName(typeVariableMapper: (TypeVariableName) -> TypeName = { it }): TypeName =
        requireNotNull(tag<ClassName>()).let { className ->
            if (typeVariables.isEmpty()) {
                className
            } else {
                className.parameterizedBy(typeVariables
                    // Removes the variance
                    .map { typeVariableMapper(TypeVariableName(it.name, *it.bounds.toTypedArray())) })
            }
        }

    fun generateAdapter(
        sourceVersion: SourceVersion,
        filer: Filer
    ): GeneratedAdapter {
        when {
            metadata.isInner ->
                throw KaptProcessingError("@JsonSerializable can't be applied to inner classes", targetElement)
            metadata.flags.isLocal ->
                throw KaptProcessingError("@JsonSerializable can't be applied to local classes", targetElement)
            !metadata.flags.isPublic && !metadata.flags.isInternal ->
                throw KaptProcessingError(
                    "Classes annotated with @JsonSerializable must public or internal",
                    targetElement
                )
        }

        val generatedAdapter = getGenerableAdapter().createRenderer().render {
            addOriginatingElement(targetElement)
            maybeAddGeneratedAnnotation(elements, sourceVersion)
        }

        generatedAdapter.fileSpec.writeTo(filer)
        return generatedAdapter
    }

    protected abstract fun getGenerableAdapter(): GeneratableJsonAdapter

    protected fun getPolymorphicLabels(): Map<String, String> =
        targetElement.getPolymorphicLabels(
            supertypes = { supertypes() },
            hasAnnotation = { getAnnotation(it) != null },
            getPolymorphicLabelKey = { getAnnotation(Polymorphic::class.java)?.labelKey },
            getPolymorphicLabel = { getAnnotation(PolymorphicLabel::class.java)?.value },
            error = ::KaptProcessingError
        )


    private fun TypeElement.supertypes(): Sequence<TypeElement> = sequence {
        if (superclass.kind == TypeKind.DECLARED) {
            val superclass = MoreElements.asType(types.asElement(superclass))
            yield(superclass)
            yieldAll(superclass.supertypes())
        }
    }
}