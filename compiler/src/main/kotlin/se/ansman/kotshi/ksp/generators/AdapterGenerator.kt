package se.ansman.kotshi.ksp.generators

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.isInternal
import com.google.devtools.ksp.isLocal
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import se.ansman.kotshi.Polymorphic
import se.ansman.kotshi.PolymorphicLabel
import se.ansman.kotshi.getPolymorphicLabels
import se.ansman.kotshi.ksp.KspProcessingError
import se.ansman.kotshi.ksp.getAnnotation
import se.ansman.kotshi.ksp.getValue
import se.ansman.kotshi.ksp.toTypeParameterResolver
import se.ansman.kotshi.model.GeneratableJsonAdapter
import se.ansman.kotshi.model.GeneratedAdapter
import se.ansman.kotshi.model.GlobalConfig
import se.ansman.kotshi.renderer.createRenderer

@Suppress("UnstableApiUsage")
abstract class AdapterGenerator(
    protected val environment: SymbolProcessorEnvironment,
    protected val resolver: Resolver,
    protected val targetElement: KSClassDeclaration,
    protected val globalConfig: GlobalConfig,
) {
    protected val typeParameterResolver = targetElement.toTypeParameterResolver()
    protected val targetClassName = targetElement.toClassName()
    protected val targetTypeVariables = targetElement.typeParameters.map { typeParameter ->
        TypeVariableName(
            typeParameter.name.getShortName(),
            typeParameter.bounds
                .map { it.resolve().toTypeName(typeParameterResolver) }
                .toList(),
        )
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
            hasAnnotation = { getAnnotation(it) != null },
            getPolymorphicLabelKey = { getAnnotation(Polymorphic::class.java)?.getValue("labelKey") },
            getPolymorphicLabel = { getAnnotation(PolymorphicLabel::class.java)?.getValue("value") },
            error = ::KspProcessingError
        )
    }

    fun generateAdapter(): GeneratedAdapter {
        when {
            Modifier.INNER in targetElement.modifiers ->
                throw KspProcessingError("@JsonSerializable can't be applied to inner classes", targetElement)
            targetElement.isLocal() ->
                throw KspProcessingError("@JsonSerializable can't be applied to local classes", targetElement)
            !targetElement.isPublic() && !targetElement.isInternal() ->
                throw KspProcessingError(
                    "Classes annotated with @JsonSerializable must public or internal",
                    targetElement
                )
        }

        val generatedAdapter = getGenerableAdapter().createRenderer().render {
            addOriginatingKSFile(targetElement.containingFile!!)
            // TODO
//            maybeAddGeneratedAnnotation(elements, sourceVersion)
        }

        generatedAdapter.fileSpec.writeTo(environment.codeGenerator, aggregating = false)
        return generatedAdapter
    }

    protected abstract fun getGenerableAdapter(): GeneratableJsonAdapter
}