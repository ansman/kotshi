package se.ansman.kotshi

import com.google.auto.common.BasicAnnotationProcessor
import com.google.common.collect.SetMultimap
import javax.annotation.processing.Messager
import javax.lang.model.element.Element
import javax.lang.model.util.Types
import javax.tools.Diagnostic

class DefaultValuesProcessingStep(
        private val messager: Messager,
        private val types: Types,
        private val defaultValueProviders: DefaultValueProviders
) : BasicAnnotationProcessor.ProcessingStep {
    override fun annotations(): Set<Class<out Annotation>> =
            setOf(JsonDefaultValueProvider::class.java)

    override fun process(elementsByAnnotation: SetMultimap<Class<out Annotation>, Element>): Set<Element> {
        for (element in elementsByAnnotation[JsonDefaultValueProvider::class.java]) {
            try {
                defaultValueProviders.register(DefaultValueProvider(types, element))
            } catch (e: ProcessingError) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Kotshi: ${e.message}", e.element)
            }
        }

        try {
            defaultValueProviders.validate()
        } catch (e: ProcessingError) {
            messager.printMessage(Diagnostic.Kind.ERROR, "Kotshi: ${e.message}", e.element)
        }
        return emptySet()
    }
}