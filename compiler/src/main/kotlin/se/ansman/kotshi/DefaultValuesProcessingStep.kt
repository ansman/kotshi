package se.ansman.kotshi

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
                            ElementKind.METHOD -> defaultValueProviders.register(DefaultValueProvider(types, e))
                            else -> {
                            }
                        }
                    }
                }
                else -> defaultValueProviders.register(DefaultValueProvider(types, element))
            }
        } catch (e: ProcessingError) {
            messager.printMessage(Diagnostic.Kind.ERROR, "Kotshi: ${e.message}", e.element)
        }
    }
}