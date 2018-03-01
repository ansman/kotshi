package se.ansman.kotshi

import com.google.auto.common.SuperficialValidation.validateElement
import com.google.auto.service.AutoService
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSetMultimap
import com.google.common.collect.Multimaps
import com.google.common.collect.SetMultimap
import com.squareup.javapoet.TypeName
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.PackageElement
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import javax.lang.model.util.SimpleElementVisitor6

@AutoService(Processor::class)
class KotshiProcessor : AbstractProcessor() {
    private lateinit var elements: Elements
    private lateinit var messager: Messager
    private lateinit var steps: ImmutableList<out ProcessingStep>
    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latestSupported()

    private fun initSteps(): Iterable<ProcessingStep> {
        val adapters: MutableMap<TypeName, GeneratedAdapter> = mutableMapOf()
        val defaultValueProviders = DefaultValueProviders(processingEnv.typeUtils)
        return listOf(
            DefaultValuesProcessingStep(
                messager = processingEnv.messager,
                types = processingEnv.typeUtils,
                defaultValueProviders = defaultValueProviders
            ),
            AdaptersProcessingStep(
                messager = processingEnv.messager,
                types = processingEnv.typeUtils,
                filer = processingEnv.filer,
                adapters = adapters,
                defaultValueProviders = defaultValueProviders
            ),
            FactoryProcessingStep(
                messager = processingEnv.messager,
                filer = processingEnv.filer,
                types = processingEnv.typeUtils,
                elements = processingEnv.elementUtils,
                adapters = adapters))
    }

    @Synchronized
    override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)
        elements = processingEnv.elementUtils
        messager = processingEnv.messager
        steps = ImmutableList.copyOf(initSteps())
    }

    private fun getSupportedAnnotationClasses(): Set<Class<out Annotation>> =
        steps.flatMapTo(mutableSetOf()) { it.annotations }

    /**
     * Returns the set of supported annotation types as a  collected from registered
     * [processing steps][ProcessingStep].
     */
    override fun getSupportedAnnotationTypes(): Set<String> =
        getSupportedAnnotationClasses().mapTo(mutableSetOf()) { it.canonicalName }

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        if (!roundEnv.processingOver()) {
            process(validElements(roundEnv), roundEnv)
        }
        return false
    }

    private fun validElements(roundEnv: RoundEnvironment): ImmutableSetMultimap<Class<out Annotation>, Element> {
        val validElements = ImmutableSetMultimap.builder<Class<out Annotation>, Element>()

        val validElementNames = LinkedHashSet<ElementName>()

        // Look at the elements we've found and the new elements from this round and validate them.
        for (annotationClass in getSupportedAnnotationClasses()) {
            // This should just call roundEnv.getElementsAnnotatedWith(Class) directly, but there is a bug
            // in some versions of eclipse that cause that method to crash.
            val annotationType = elements.getTypeElement(annotationClass.canonicalName)
            val elementsAnnotatedWith = if (annotationType == null) {
                emptySet()
            } else {
                roundEnv.getElementsAnnotatedWith(annotationType)
            }
            for (annotatedElement in elementsAnnotatedWith) {
                if (annotatedElement.kind == ElementKind.PACKAGE) {
                    val annotatedPackageElement = annotatedElement as PackageElement
                    val annotatedPackageName = ElementName.forPackageName(annotatedPackageElement.qualifiedName.toString())
                    val validPackage = validElementNames.contains(annotatedPackageName) || validateElement(annotatedPackageElement)
                    if (validPackage) {
                        validElements.put(annotationClass, annotatedPackageElement)
                        validElementNames.add(annotatedPackageName)
                    }
                } else {
                    val enclosingType = getEnclosingType(annotatedElement)
                    val enclosingTypeName = ElementName.forTypeName(enclosingType.qualifiedName.toString())
                    val validEnclosingType = validElementNames.contains(enclosingTypeName) || validateElement(enclosingType)
                    if (validEnclosingType) {
                        validElements.put(annotationClass, annotatedElement)
                        validElementNames.add(enclosingTypeName)
                    }
                }
            }
        }

        return validElements.build()
    }

    /** Processes the valid elements, including those previously deferred by each step.  */
    private fun process(validElements: ImmutableSetMultimap<Class<out Annotation>, Element>, roundEnv: RoundEnvironment) {
        for (step in steps) {
            val stepElements = Multimaps.filterKeys(validElements, { it in step.annotations })
            if (!stepElements.isEmpty) {
                step.process(stepElements, roundEnv)
            }
        }
    }

    interface ProcessingStep {
        val annotations: Set<Class<out Annotation>>

        fun process(elementsByAnnotation: SetMultimap<Class<out Annotation>, Element>, roundEnv: RoundEnvironment)
    }

    private data class ElementName(private val kind: ElementName.Kind, val name: String) {
        private enum class Kind {
            PACKAGE_NAME,
            TYPE_NAME
        }

        companion object {
            /**
             * An [ElementName] for a package.
             */
            internal fun forPackageName(packageName: String): ElementName = ElementName(Kind.PACKAGE_NAME, packageName)

            /**
             * An [ElementName] for a type.
             */
            internal fun forTypeName(typeName: String): ElementName = ElementName(Kind.TYPE_NAME, typeName)

        }
    }
}


/**
 * Returns the nearest enclosing [TypeElement] to the current element, throwing
 * an [IllegalArgumentException] if the provided [Element] is a
 * [PackageElement] or is otherwise not enclosed by a type.
 */
private fun getEnclosingType(element: Element): TypeElement {
    return element.accept(object : SimpleElementVisitor6<TypeElement, Void>() {
        override fun defaultAction(e: Element, p: Void?): TypeElement = e.enclosingElement.accept(this, p)

        override fun visitType(e: TypeElement, p: Void?): TypeElement = e

        override fun visitPackage(e: PackageElement, p: Void?): Nothing {
            throw IllegalArgumentException()
        }
    }, null)
}