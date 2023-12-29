package se.ansman.kotshi.ksp.generators

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.isInternal
import com.google.devtools.ksp.isLocal
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeVariableName
import com.squareup.kotlinpoet.ksp.writeTo
import se.ansman.kotshi.Errors
import se.ansman.kotshi.Polymorphic
import se.ansman.kotshi.PolymorphicLabel
import se.ansman.kotshi.ProguardConfig
import se.ansman.kotshi.getPolymorphicLabels
import se.ansman.kotshi.ksp.KspProcessingError
import se.ansman.kotshi.ksp.getAnnotation
import se.ansman.kotshi.ksp.getValue
import se.ansman.kotshi.ksp.toTypeParameterResolver
import se.ansman.kotshi.model.GeneratableJsonAdapter
import se.ansman.kotshi.model.GeneratedAdapter
import se.ansman.kotshi.model.GeneratedAnnotation
import se.ansman.kotshi.model.GlobalConfig
import se.ansman.kotshi.renderer.createRenderer
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

abstract class AdapterGenerator(
    protected val environment: SymbolProcessorEnvironment,
    protected val targetElement: KSClassDeclaration,
    protected val globalConfig: GlobalConfig,
    protected val resolver: Resolver,
) {
    @Suppress("unused")
    protected val logger get() = environment.logger
    protected val typeParameterResolver = targetElement.toTypeParameterResolver()
    protected val targetClassName = targetElement.toClassName()
    protected val targetTypeVariables = targetElement.typeParameters.map {
        it.toTypeVariableName(typeParameterResolver)
    }

    protected val polymorphicLabels: Map<String, String> by lazy {
        targetElement.getPolymorphicLabels(
            supertypes = {
                getAllSuperTypes()
                    .map { type ->
                        type.declaration as? KSClassDeclaration
                            ?: throw KspProcessingError(
                                "Unknown super type ${type.declaration.javaClass}",
                                type.declaration
                            )
                    }
            },
            getPolymorphicLabelKey = { getAnnotation(Polymorphic::class.java)?.getValue("labelKey") }
        ) { getAnnotation(PolymorphicLabel::class.java)?.getValue("value") }
    }

    fun generateAdapter(
        createAnnotationsUsingConstructor: Boolean,
        generatedAnnotation: GeneratedAnnotation?,
    ): GeneratedAdapter<KSFile> {
        when {
            Modifier.INNER in targetElement.modifiers ->
                throw KspProcessingError(Errors.dataClassCannotBeInner, targetElement)

            targetElement.isLocal() ->
                throw KspProcessingError(Errors.dataClassCannotBeLocal, targetElement)

            !targetElement.isPublic() && !targetElement.isInternal() ->
                throw KspProcessingError(Errors.privateClass, targetElement)
        }

        val generatedAdapter = getGeneratableJsonAdapter()
            .createRenderer(
                createAnnotationsUsingConstructor = createAnnotationsUsingConstructor,
                error = { KspProcessingError(it, targetElement) },
            )
            .render(generatedAnnotation, originatingElement = targetElement.containingFile!!) {
                addOriginatingKSFile(targetElement.containingFile!!)
            }

        generatedAdapter.fileSpec.writeTo(environment.codeGenerator, aggregating = false)
        generatedAdapter.proguardConfig?.writeTo(environment.codeGenerator, targetElement.containingFile!!)
        return generatedAdapter
    }

    protected abstract fun getGeneratableJsonAdapter(): GeneratableJsonAdapter
}

/** Writes this config to a [codeGenerator]. */
private fun ProguardConfig.writeTo(codeGenerator: CodeGenerator, originatingKSFile: KSFile) {
    val file = codeGenerator.createNewFile(
        dependencies = Dependencies(aggregating = false, originatingKSFile),
        packageName = "",
        fileName = outputFilePathWithoutExtension(targetClass.canonicalName),
        extensionName = "pro"
    )
    // Don't use writeTo(file) because that tries to handle directories under the hood
    OutputStreamWriter(file, StandardCharsets.UTF_8)
        .use(::writeTo)
}