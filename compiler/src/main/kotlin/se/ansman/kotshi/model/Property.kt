package se.ansman.kotshi.model

import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.ARRAY
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.WildcardTypeName
import se.ansman.kotshi.Errors
import se.ansman.kotshi.Functions
import se.ansman.kotshi.PrimitiveAdapters
import se.ansman.kotshi.isPrimitive
import se.ansman.kotshi.notNull
import se.ansman.kotshi.unwrapTypeAlias

data class Property(
    val name: String,
    val type: TypeName,
    val jsonName: String,
    val jsonQualifiers: Set<AnnotationModel>,
    val hasDefaultValue: Boolean,
    val shouldUseAdapter: Boolean,
    val isIgnored: Boolean
) {
    val variableName = name.replaceFirstChar(Char::lowercaseChar)

    init {
        if (isIgnored) {
            require(hasDefaultValue)
        }
    }

    companion object {
        fun create(
            name: String,
            type: TypeName,
            jsonQualifiers: Collection<AnnotationModel>,
            globalConfig: GlobalConfig,
            useAdaptersForPrimitives: PrimitiveAdapters,
            parameterJsonName: String?,
            propertyJsonName: String?,
            isTransient: Boolean,
            isJsonIgnore: Boolean?,
            hasDefaultValue: Boolean,
            error: (String) -> Throwable
        ): Property {
            if (isJsonIgnore == false && isTransient) {
                throw error(Errors.nonIgnoredDataClassPropertyMustNotBeTransient(name))
            }

            if (!hasDefaultValue) {
                if (isTransient) {
                    throw error(Errors.transientDataClassPropertyWithoutDefaultValue(name))
                } else if (isJsonIgnore == true) {
                    throw error(Errors.ignoredDataClassPropertyWithoutDefaultValue(name))
                }
            }

            return Property(
                name = name,
                type = type,
                jsonName = parameterJsonName ?: propertyJsonName ?: name,
                jsonQualifiers = jsonQualifiers.toSet(),
                isIgnored = isTransient || isJsonIgnore == true,
                hasDefaultValue = hasDefaultValue,
                shouldUseAdapter = jsonQualifiers.isNotEmpty() ||
                    !type.notNull().isPrimitive && type.notNull() != STRING ||
                    when (useAdaptersForPrimitives) {
                        PrimitiveAdapters.DEFAULT -> globalConfig.useAdaptersForPrimitives
                        PrimitiveAdapters.ENABLED -> true
                        PrimitiveAdapters.DISABLED -> false
                    }
            )
        }
    }
}


fun Property.asRuntimeType(typeVariableAccessor: (TypeVariableName) -> CodeBlock): CodeBlock =
    type.asRuntimeType(typeVariableAccessor)

val Property.suggestedAdapterName: String
    get() = "${variableName}Adapter"

private fun TypeName.asRuntimeType(typeVariableAccessor: (TypeVariableName) -> CodeBlock): CodeBlock =
    when (val type = unwrapTypeAlias()) {
        is ParameterizedTypeName ->
            CodeBlock.builder()
                .add(
                    "%M(%T::class.javaObjectType", Functions.Moshi.newParameterizedType, if (type.rawType == ARRAY) {
                        // Arrays are special, you cannot just do Array::class.java
                        this
                    } else {
                        type.rawType.notNull()
                    }
                )
                .apply {
                    for (typeArgument in type.typeArguments) {
                        add(", ")
                        add(typeArgument.asRuntimeType(typeVariableAccessor))
                    }
                }
                .add(")")
                .build()

        is WildcardTypeName -> when {
            type.inTypes.size == 1 -> type.inTypes[0].asRuntimeType(typeVariableAccessor)
            type.outTypes[0] == ANY -> ANY.asRuntimeType(typeVariableAccessor)
            else -> type.outTypes[0].asRuntimeType(typeVariableAccessor)
        }

        is LambdaTypeName -> type.asParameterizedTypeName().asRuntimeType(typeVariableAccessor)
        is TypeVariableName -> typeVariableAccessor(type)
        else -> CodeBlock.of("%T::class.javaObjectType", type.notNull())
    }

private fun LambdaTypeName.asParameterizedTypeName(): ParameterizedTypeName {
    val parameters = mutableListOf<TypeName>()
    receiver?.let(parameters::add)
    parameters.addAll(parameters)
    parameters.add(returnType)
    return ClassName("kotlin", "Function${parameters.size + (if (receiver == null) 0 else 1)}")
        .parameterizedBy(parameters)
}