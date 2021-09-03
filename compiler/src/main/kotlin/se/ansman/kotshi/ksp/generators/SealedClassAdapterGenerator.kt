package se.ansman.kotshi.ksp.generators

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.Modifier.SEALED
import com.squareup.kotlinpoet.STAR
import se.ansman.kotshi.JsonDefaultValue
import se.ansman.kotshi.Polymorphic
import se.ansman.kotshi.Polymorphic.Fallback
import se.ansman.kotshi.PolymorphicLabel
import se.ansman.kotshi.ksp.KspProcessingError
import se.ansman.kotshi.ksp.asClassName
import se.ansman.kotshi.ksp.asTypeName
import se.ansman.kotshi.ksp.getAnnotation
import se.ansman.kotshi.ksp.getEnumValue
import se.ansman.kotshi.ksp.getValue
import se.ansman.kotshi.model.GeneratableJsonAdapter
import se.ansman.kotshi.model.GlobalConfig
import se.ansman.kotshi.model.SealedClassJsonAdapter
import se.ansman.kotshi.model.getSealedSubtypes

class SealedClassAdapterGenerator(
    environment: SymbolProcessorEnvironment,
    resolver: Resolver,
    targetElement: KSClassDeclaration,
    globalConfig: GlobalConfig
) : AdapterGenerator(environment, resolver, targetElement, globalConfig) {
    init {
        require(SEALED in targetElement.modifiers)
    }

    override fun getGenerableAdapter(): GeneratableJsonAdapter {
        val sealedSubclasses = targetElement.getAllSealedSubclasses().toList()
        val annotation = targetElement.getAnnotation(Polymorphic::class.java)!!
        val labelKey = annotation.getValue<String>("labelKey")
        val subtypes = sealedSubclasses
            .mapNotNull { it.toSubtype() }
            .toList()

        if (subtypes.isEmpty()) {
            throw KspProcessingError("No classes annotated with @PolymorphicLabel", targetElement)
        }

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
                        throw KspProcessingError("The default value of a sealed class cannot be generic", it)
                    }
                    it.asClassName()
                }
                .toList()
                .let { defaultValues ->
                    if (defaultValues.size > 1) {
                        throw KspProcessingError("Multiple subclasses annotated with @JsonDefaultValue", targetElement)
                    } else {
                        defaultValues.singleOrNull()
                    }
                }
        )
    }

    private fun KSClassDeclaration.toSubtype(): SealedClassJsonAdapter.Subtype? {
        return SealedClassJsonAdapter.Subtype(
            type = asTypeName(),
            wildcardType = asTypeName(typeParameters.map { STAR }),
            superClass = superTypes
                .filter { (it.resolve().declaration as? KSClassDeclaration)?.classKind == ClassKind.CLASS }
                // All subtypes should have a superclass
                .first()
                .asTypeName(),
            label = getAnnotation(PolymorphicLabel::class.java)?.getValue("value")
                ?: run {
                    if (SEALED !in modifiers && getAnnotation<JsonDefaultValue>() == null) {
                        throw KspProcessingError("Missing @PolymorphicLabel on ${asClassName()}", this)
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