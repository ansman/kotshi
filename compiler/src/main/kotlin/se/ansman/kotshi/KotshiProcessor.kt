package se.ansman.kotshi

import com.google.auto.common.BasicAnnotationProcessor
import com.google.auto.service.AutoService
import com.squareup.javapoet.TypeName
import javax.annotation.processing.Processor
import javax.lang.model.SourceVersion

@AutoService(Processor::class)
class KotshiProcessor : BasicAnnotationProcessor() {
    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latestSupported()

    override fun initSteps(): Iterable<ProcessingStep> {
        val adapters: MutableMap<TypeName, TypeName> = mutableMapOf()
        return listOf(
                AdaptersProcessingStep(
                        messager = processingEnv.messager,
                        filer = processingEnv.filer,
                        adapters = adapters
                ),
                FactoryProcessingStep(
                        messager = processingEnv.messager,
                        filer = processingEnv.filer,
                        types = processingEnv.typeUtils,
                        elements = processingEnv.elementUtils,
                        adapters = adapters))
    }
}