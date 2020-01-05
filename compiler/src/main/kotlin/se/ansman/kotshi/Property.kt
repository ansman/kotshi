package se.ansman.kotshi

import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import javax.lang.model.element.ElementKind
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements

class Property private constructor(
    val name: String,
    val type: TypeName,
    val jsonName: String,
    val adapterKey: AdapterKey,
    val parameter: ParameterSpec,
    val shouldUseAdapter: Boolean,
    val isTransient: Boolean
) {
    val hasDefaultValue: Boolean = parameter.defaultValue != null

    companion object {
        fun create(
            elements: Elements,
            typeSpec: TypeSpec,
            globalConfig: GlobalConfig,
            enclosingClass: TypeElement,
            parameter: ParameterSpec
        ): Property {
            val name = parameter.name
            val type = parameter.type
            val adapterKey = AdapterKey(type.notNull(), parameter.annotations.qualifiers(elements))

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


            KModifier.PRIVATE in property.modifiers || KModifier.PROTECTED in property.modifiers
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
                parameter = parameter,
                shouldUseAdapter = useAdaptersForPrimitives ||
                    adapterKey.jsonQualifiers.isNotEmpty() ||
                    !type.notNull().isPrimitive && type.notNull() != STRING,
                isTransient = isTransient
            )
        }
    }
}