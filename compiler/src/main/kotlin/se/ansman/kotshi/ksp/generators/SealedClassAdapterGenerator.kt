package se.ansman.kotshi.ksp.generators

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.Modifier.SEALED
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import se.ansman.kotshi.Errors.defaultSealedValueIsGeneric
import se.ansman.kotshi.Errors.multipleJsonDefaultValueInSealedClass
import se.ansman.kotshi.Errors.noSealedSubclasses
import se.ansman.kotshi.Errors.polymorphicSubclassMustHavePolymorphicLabel
import se.ansman.kotshi.JsonDefaultValue
import se.ansman.kotshi.Polymorphic
import se.ansman.kotshi.Polymorphic.Fallback
import se.ansman.kotshi.PolymorphicLabel
import se.ansman.kotshi.ksp.KspProcessingError
import se.ansman.kotshi.ksp.asTypeName
import se.ansman.kotshi.ksp.getAnnotation
import se.ansman.kotshi.ksp.getEnumValue
import se.ansman.kotshi.ksp.getValue
import se.ansman.kotshi.ksp.toTypeParameterResolver
import se.ansman.kotshi.model.GeneratableJsonAdapter
import se.ansman.kotshi.model.GlobalConfig
import se.ansman.kotshi.model.SealedClassJsonAdapter
import se.ansman.kotshi.model.getSealedSubtypes

class SealedClassAdapterGenerator(
    environment: SymbolProcessorEnvironment,
    targetElement: KSClassDeclaration,
    globalConfig: GlobalConfig,
    resolver: Resolver
) : AdapterGenerator(environment, targetElement, globalConfig, resolver) {
    init {
        require(SEALED in targetElement.modifiers)
    }

    override fun getGeneratableJsonAdapter(): GeneratableJsonAdapter {
        val sealedSubclasses = targetElement.getAllSealedSubclasses().toList()
        if (sealedSubclasses.isEmpty()) {
            throw KspProcessingError(noSealedSubclasses, targetElement)
        }
        val annotation = targetElement.getAnnotation(Polymorphic::class.java)!!
        val labelKey = annotation.getValue<String>("labelKey")
        val subtypes = sealedSubclasses
            .mapNotNull { it.toSubtype() }
            .toList()


        return SealedClassJsonAdapter(
            targetPackageName = targetClassName.packageName,
            targetSimpleNames = targetClassName.simpleNames,
            targetTypeVariables = targetTypeVariables,
            polymorphicLabels = polymorphicLabels,
            labelKey = labelKey,
            onMissing = annotation.getEnumValue("onMissing", Fallback.DEFAULT),
            onInvalid = annotation.getEnumValue("onInvalid", Fallback.DEFAULT),
            subtypes = subtypes,
            defaultType = sealedSubclasses
                .filter { it.getAnnotation<JsonDefaultValue>() != null }
                .map {
                    if (it.typeParameters.isNotEmpty()) {
                        throw KspProcessingError(defaultSealedValueIsGeneric, it)
                    }
                    it.toClassName()
                }
                .toList()
                .let { defaultValues ->
                    if (defaultValues.size > 1) {
                        throw KspProcessingError(multipleJsonDefaultValueInSealedClass, targetElement)
                    } else {
                        defaultValues.singleOrNull()
                    }
                }
        )
    }

    private fun KSClassDeclaration.toSubtype(): SealedClassJsonAdapter.Subtype? {
        val typeParameterResolver = toTypeParameterResolver()
        return SealedClassJsonAdapter.Subtype(
            type = asTypeName(typeParameterResolver),
            wildcardType = asTypeName(typeParameterResolver, typeParameters.map { STAR }),
            superClass = superTypes
                .filter { (it.resolve().declaration as? KSClassDeclaration)?.classKind == ClassKind.CLASS }
                // All subtypes should have a superclass
                .first()
                .toTypeName(typeParameterResolver),
            label = getAnnotation(PolymorphicLabel::class.java)?.getValue("value")
                ?: run {
                    if (SEALED !in modifiers && getAnnotation<JsonDefaultValue>() == null) {
                        throw KspProcessingError(polymorphicSubclassMustHavePolymorphicLabel, this)
                    }
                    return null
                }
        )
    }

    private fun KSClassDeclaration.getAllSealedSubclasses(): Sequence<KSClassDeclaration> =
        getSealedSubtypes(
            getSealedSubclasses = KSClassDeclaration::getSealedSubclasses,
            isSealed = { SEALED in modifiers },
            hasAnnotation = { getAnnotation(it) != null },
            getPolymorphicLabelKey = { getAnnotation<Polymorphic>()?.getValue("labelKey") },
            getPolymorphicLabel = { getAnnotation<PolymorphicLabel>()?.getValue("value") },
            error = ::KspProcessingError,
        )
}