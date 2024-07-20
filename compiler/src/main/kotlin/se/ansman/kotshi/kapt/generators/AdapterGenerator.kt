package se.ansman.kotshi.kapt.generators

import com.google.auto.common.MoreElements
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.tag
import kotlin.metadata.KmClass
import kotlin.metadata.Visibility
import kotlin.metadata.isInner
import kotlin.metadata.visibility
import se.ansman.kotshi.Errors
import se.ansman.kotshi.Polymorphic
import se.ansman.kotshi.PolymorphicLabel
import se.ansman.kotshi.ProguardConfig
import se.ansman.kotshi.getPolymorphicLabels
import se.ansman.kotshi.kapt.KaptProcessingError
import se.ansman.kotshi.kapt.MetadataAccessor
import se.ansman.kotshi.kapt.supportsCreatingAnnotationsWithConstructor
import se.ansman.kotshi.model.GeneratableJsonAdapter
import se.ansman.kotshi.model.GeneratedAdapter
import se.ansman.kotshi.model.GeneratedAnnotation
import se.ansman.kotshi.model.GlobalConfig
import se.ansman.kotshi.renderer.createRenderer
import javax.annotation.processing.Filer
import javax.annotation.processing.Messager
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeKind
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import javax.tools.StandardLocation

@Suppress("UnstableApiUsage")
abstract class AdapterGenerator(
    protected val metadataAccessor: MetadataAccessor,
    protected val types: Types,
    protected val elements: Elements,
    protected val targetElement: TypeElement,
    protected val kmClass: KmClass,
    protected val globalConfig: GlobalConfig,
    @Suppress("unused") // Useful to have for debugging
    protected val messager: Messager,
) {
    protected val targetTypeSpec = metadataAccessor.getTypeSpec(kmClass)

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
        generatedAnnotation: GeneratedAnnotation?,
        filer: Filer,
    ): GeneratedAdapter<Element> {
        when {
            kmClass.isInner ->
                throw KaptProcessingError(Errors.dataClassCannotBeInner, targetElement)
            kmClass.visibility == Visibility.LOCAL ->
                throw KaptProcessingError(Errors.dataClassCannotBeLocal, targetElement)
            kmClass.visibility != Visibility.PUBLIC && kmClass.visibility != Visibility.INTERNAL->
                throw KaptProcessingError(Errors.privateClass, targetElement)
        }

        val generatedAdapter = getGeneratableJsonAdapter()
            .createRenderer(
                createAnnotationsUsingConstructor = metadataAccessor.getMetadata(targetElement).supportsCreatingAnnotationsWithConstructor,
                error = { KaptProcessingError(it, targetElement) },
            )
            .render(generatedAnnotation, originatingElement = targetElement) {
                addOriginatingElement(targetElement)
            }

        generatedAdapter.fileSpec.writeTo(filer)
        generatedAdapter.proguardConfig?.writeTo(filer, targetElement)
        return generatedAdapter
    }

    protected abstract fun getGeneratableJsonAdapter(): GeneratableJsonAdapter

    protected fun getPolymorphicLabels(): Map<String, String> =
        targetElement.getPolymorphicLabels(
            supertypes = { supertypes() },
            getPolymorphicLabelKey = { getAnnotation(Polymorphic::class.java)?.labelKey }
        ) { getAnnotation(PolymorphicLabel::class.java)?.value }


    private fun TypeElement.supertypes(): Sequence<TypeElement> = sequence {
        if (superclass.kind == TypeKind.DECLARED) {
            val superclass = MoreElements.asType(types.asElement(superclass))
            yield(superclass)
            yieldAll(superclass.supertypes())
        }
    }
}

/** Writes this config to a [filer]. */
private fun ProguardConfig.writeTo(filer: Filer, vararg originatingElements: Element) {
    filer.createResource(StandardLocation.CLASS_OUTPUT, "", "${outputFilePathWithoutExtension(targetClass.canonicalName)}.pro", *originatingElements)
        .openWriter()
        .use(::writeTo)
}