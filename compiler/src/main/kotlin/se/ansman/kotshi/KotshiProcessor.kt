package se.ansman.kotshi

import com.google.auto.common.SuperficialValidation.validateElement
import com.google.auto.service.AutoService
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSetMultimap
import com.google.common.collect.Multimaps
import com.google.common.collect.SetMultimap
import me.eugeniomarletti.kotlin.metadata.KotlinMetadataUtils
import me.eugeniomarletti.kotlin.processing.KotlinAbstractProcessor
import net.ltgt.gradle.incap.IncrementalAnnotationProcessor
import net.ltgt.gradle.incap.IncrementalAnnotationProcessorType.AGGREGATING
import javax.annotation.processing.Filer
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
@IncrementalAnnotationProcessor(AGGREGATING)
class KotshiProcessor : KotlinAbstractProcessor(), KotlinMetadataUtils {
    private lateinit var elements: Elements
    private lateinit var steps: ImmutableList<out ProcessingStep>
    private val processingEnvironment: ProcessingEnvironment
        get() = (this as KotlinAbstractProcessor).processingEnv

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latestSupported()

    private fun initSteps(): Iterable<ProcessingStep> {
        val adapters: MutableList<GeneratedAdapter> = mutableListOf()
        return listOf(
            AdaptersProcessingStep(
                processor = this,
                messager = processingEnvironment.messager,
                filer = processingEnvironment.filer,
                adapters = adapters,
                elements = processingEnvironment.elementUtils,
                sourceVersion = processingEnvironment.sourceVersion
            ),
            FactoryProcessingStep(
                processor = this,
                messager = processingEnvironment.messager,
                filer = processingEnvironment.filer,
                types = processingEnvironment.typeUtils,
                elements = processingEnvironment.elementUtils,
                sourceVersion = processingEnvironment.sourceVersion,
                adapters = adapters
            )
        )
    }

    @Synchronized
    override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)
        elements = processingEnv.elementUtils
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
            val stepElements = Multimaps.filterKeys(validElements) { it in step.annotations }
            if (!stepElements.isEmpty) {
                step.process(stepElements, roundEnv)
            }
        }
    }

    interface ProcessingStep {
        val annotations: Set<Class<out Annotation>>
        fun process(elementsByAnnotation: SetMultimap<Class<out Annotation>, Element>, roundEnv: RoundEnvironment)
    }

    abstract class GeneratingProcessingStep : ProcessingStep {
        protected abstract val filer: Filer
        protected abstract val processor: KotshiProcessor
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