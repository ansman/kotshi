package se.ansman.kotshi

import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import com.google.common.collect.SetMultimap
import javax.annotation.processing.Messager
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.util.Types
import javax.tools.Diagnostic

class DefaultValuesProcessingStep(
    private val messager: Messager,
    private val types: Types,
    private val defaultValueProviders: DefaultValueProviders
) : KotshiProcessor.ProcessingStep {
    override val annotations: Set<Class<out Annotation>> = setOf(JsonDefaultValue::class.java)

    override fun process(elementsByAnnotation: SetMultimap<Class<out Annotation>, Element>, roundEnv: RoundEnvironment) {
        for (element in elementsByAnnotation[JsonDefaultValue::class.java]) {
            processElement(element, roundEnv)
        }

        try {
            defaultValueProviders.validate()
        } catch (e: ProcessingError) {
            messager.printMessage(Diagnostic.Kind.ERROR, "Kotshi: ${e.message}", e.element)
        }
    }

    private fun processElement(element: Element, roundEnv: RoundEnvironment) {
        try {
            when (element.kind) {
                ElementKind.PARAMETER -> return
                ElementKind.ANNOTATION_TYPE -> {
                    for (e in roundEnv.getElementsAnnotatedWith(MoreTypes.asTypeElement(element.asType()))) {
                        when (e.kind) {
                            ElementKind.CONSTRUCTOR,
                            ElementKind.FIELD,
                            ElementKind.METHOD -> defaultValueProviders.register(ComplexDefaultValueProvider(types, e.getProvider()))
                            else -> {
                            }
                        }
                    }
                }
                else -> defaultValueProviders.register(ComplexDefaultValueProvider(types, element.getProvider()))
            }
        } catch (e: ProcessingError) {
            messager.printMessage(Diagnostic.Kind.ERROR, "Kotshi: ${e.message}", e.element)
        }
    }

    private fun Element.getProvider(): Element =
        if (isPublic) {
            this
        } else {
            when (kind) {
                ElementKind.FIELD -> {
                    val getterName = MoreElements.asVariable(this).getGetterName()
                    enclosingElement
                        .findMethodNamed(getterName)
                        ?: enclosingElement?.findClassNamed("Companion")?.findMethodNamed(getterName)
                        ?: this
                }
                else -> this
            }
        }

    private fun Element.findMethodNamed(name: String): Element? =
        enclosedElements
            .asSequence()
            .filter { it.kind == ElementKind.METHOD }
            .firstOrNull { it.simpleName.contentEquals(name) }

    private fun Element.findClassNamed(name: String): Element? =
        enclosedElements
            .asSequence()
            .filter { it.kind == ElementKind.CLASS }
            .firstOrNull { it.simpleName.contentEquals(name) }
}