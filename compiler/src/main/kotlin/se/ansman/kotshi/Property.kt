package se.ansman.kotshi

import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Modifier.PRIVATE
import com.google.devtools.ksp.symbol.Modifier.PROTECTED
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.moshi.Json
import se.ansman.kotshi.kapt.ProcessingError
import se.ansman.kotshi.ksp.KspProcessingError
import se.ansman.kotshi.ksp.getAnnotation
import se.ansman.kotshi.ksp.getEnumValue
import se.ansman.kotshi.ksp.getValue
import se.ansman.kotshi.ksp.isJsonQualifier
import se.ansman.kotshi.ksp.toAnnotationSpec
import se.ansman.kotshi.ksp.toTypeName
import javax.lang.model.element.ElementKind
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements

class Property private constructor(
    val name: String,
    val type: TypeName,
    val jsonName: String,
    val adapterKey: AdapterKey,
    val hasDefaultValue: Boolean,
    val shouldUseAdapter: Boolean,
    val isTransient: Boolean
) {

    companion object {
        fun create(
            elements: Elements,
            typeSpec: TypeSpec,
            globalConfig: GlobalConfig,
            enclosingClass: TypeElement,
            parameter: ParameterSpec
        ): Property {
            val name = parameter.name
            val type = parameter.type.unwrapTypeAlias()

            val adapterKey = AdapterKey(
                type = type.copy(nullable = false, annotations = emptyList()),
                jsonQualifiers = parameter.annotations.qualifiers(elements)
            )

            val useAdaptersForPrimitives = when (enclosingClass.getAnnotation(JsonSerializable::class.java).useAdaptersForPrimitives) {
                PrimitiveAdapters.DEFAULT -> globalConfig.useAdaptersForPrimitives
                PrimitiveAdapters.ENABLED -> true
                PrimitiveAdapters.DISABLED -> false
            }

            val property = typeSpec.propertySpecs.find { it.name == name }
                ?: throw ProcessingError("Could not find property for parameter $name", enclosingClass)

            val field = enclosingClass.enclosedElements
                .asSequence()
                .filter { it.kind == ElementKind.FIELD && Modifier.STATIC !in it.modifiers }
                .find { it.simpleName.contentEquals(name) }


            if (KModifier.PRIVATE in property.modifiers || KModifier.PROTECTED in property.modifiers) {
                throw ProcessingError("Property $name must be public or internal", enclosingClass)
            }

            val isTransient = Modifier.TRANSIENT in field?.modifiers ?: emptySet()

            if (isTransient && parameter.defaultValue == null) {
                throw ProcessingError("Transient property $name must declare a default value", enclosingClass)
            }

            return Property(
                name = name,
                type = type,
                jsonName = parameter.annotations.jsonName()
                    ?: property.annotations.jsonName()
                    ?: name,
                adapterKey = adapterKey,
                hasDefaultValue = parameter.defaultValue != null,
                shouldUseAdapter = useAdaptersForPrimitives ||
                    adapterKey.jsonQualifiers.isNotEmpty() ||
                    !type.notNull().isPrimitive && type.notNull() != STRING,
                isTransient = isTransient
            )
        }

        fun create(
            globalConfig: GlobalConfig,
            resolver: Resolver,
            enclosingClass: KSClassDeclaration,
            parameter: KSValueParameter
        ): Property {
            val name = parameter.name!!.asString()
            val type = parameter.type.resolve().toTypeName()

            val property = enclosingClass.getDeclaredProperties().find { it.simpleName == parameter.name }
                ?: throw KspProcessingError("Could not find property for parameter $name", parameter)

            val adapterKey = AdapterKey(
                type = type.copy(nullable = false, annotations = emptyList()),
                jsonQualifiers = parameter.annotations
                    .filter { it.isJsonQualifier() }
                    .map { it.toAnnotationSpec(resolver) }
                    .toSet()
            )

            val useAdaptersForPrimitives = when (enclosingClass.getAnnotation<JsonSerializable>()!!.getEnumValue<PrimitiveAdapters>("useAdaptersForPrimitives", PrimitiveAdapters.DEFAULT)) {
                PrimitiveAdapters.DEFAULT -> globalConfig.useAdaptersForPrimitives
                PrimitiveAdapters.ENABLED -> true
                PrimitiveAdapters.DISABLED -> false
            }

            if (PRIVATE in property.modifiers || PROTECTED in property.modifiers) {
                throw KspProcessingError("Property $name must be public or internal", property)
            }

            val isTransient = property.getAnnotation<Transient>() != null

            if (isTransient && !parameter.hasDefault) {
                throw KspProcessingError("Transient property $name must declare a default value", property)
            }

            return Property(
                name = name,
                type = type,
                jsonName = parameter.getAnnotation<Json>()?.getValue("name")
                    ?: property.getAnnotation<Json>()?.getValue("name")
                    ?: name,
                adapterKey = adapterKey,
                hasDefaultValue = parameter.hasDefault,
                shouldUseAdapter = useAdaptersForPrimitives ||
                    adapterKey.jsonQualifiers.isNotEmpty() ||
                    !type.notNull().isPrimitive && type.notNull() != STRING,
                isTransient = isTransient
            )
        }

    }
}