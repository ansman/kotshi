package se.ansman.kotshi

import com.squareup.kotlinpoet.TypeName
import com.squareup.moshi.Json
import me.eugeniomarletti.kotlin.metadata.declaresDefaultValue
import me.eugeniomarletti.kotlin.metadata.shadow.metadata.ProtoBuf
import me.eugeniomarletti.kotlin.metadata.shadow.metadata.ProtoBuf.Visibility.INTERNAL
import me.eugeniomarletti.kotlin.metadata.shadow.metadata.ProtoBuf.Visibility.PUBLIC
import me.eugeniomarletti.kotlin.metadata.shadow.metadata.deserialization.NameResolver
import me.eugeniomarletti.kotlin.metadata.visibility
import javax.lang.model.element.ElementKind
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement

class Property private constructor(
    val name: String,
    val type: TypeName,
    val jsonName: String,
    val adapterKey: AdapterKey,
    val parameter: VariableElement,
    val valueParameter: ProtoBuf.ValueParameter,
    val shouldUseAdapter: Boolean,
    val isTransient: Boolean
) {
    val hasDefaultValue: Boolean = valueParameter.declaresDefaultValue

    init {
        if (isTransient && !hasDefaultValue) {
            throw ProcessingError("Transient properties must declare a default value", parameter)
        }
    }

    companion object {
        fun create(
            classProto: ProtoBuf.Class,
            nameResolver: NameResolver,
            globalConfig: GlobalConfig,
            enclosingClass: TypeElement,
            parameter: VariableElement,
            valueParameter: ProtoBuf.ValueParameter
        ): Property {
            val name = nameResolver.getString(valueParameter.name)
            val type = valueParameter.type.asTypeName(nameResolver, classProto::getTypeParameter, useAbbreviatedType = false)
            val adapterKey = AdapterKey(type.notNull(), parameter.getJsonQualifiers())

            val useAdaptersForPrimitives = when (enclosingClass.getAnnotation(JsonSerializable::class.java).useAdaptersForPrimitives) {
                PrimitiveAdapters.DEFAULT -> globalConfig.useAdaptersForPrimitives
                PrimitiveAdapters.ENABLED -> true
                PrimitiveAdapters.DISABLED -> false
            }

            val protoProperty = classProto.propertyList.find { nameResolver.getString(it.name) == name }
                ?: throw ProcessingError("Could not find property for parameter", parameter)

            val field = enclosingClass.enclosedElements
                .asSequence()
                .filter { it.kind == ElementKind.FIELD && Modifier.STATIC !in it.modifiers }
                .find { it.simpleName.contentEquals(name) }

            when (protoProperty.visibility) {
                INTERNAL, PUBLIC -> Unit
                else -> throw ProcessingError("Properties must be public or internal", parameter)
            }

            val isTransient = Modifier.TRANSIENT in field?.modifiers ?: emptySet()

            return Property(
                name = name,
                type = type,
                jsonName = parameter.getAnnotation(Json::class.java)?.name
                    ?: field?.getAnnotation(Json::class.java)?.name
                    ?: name,
                adapterKey = adapterKey,
                parameter = parameter,
                valueParameter = valueParameter,
                shouldUseAdapter = useAdaptersForPrimitives ||
                    adapterKey.jsonQualifiers.isNotEmpty() ||
                    !type.notNull().isPrimitive && type.notNull() != STRING,
                isTransient = isTransient
            )
        }
    }
}