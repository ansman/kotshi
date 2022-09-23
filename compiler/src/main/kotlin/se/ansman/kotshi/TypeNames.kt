package se.ansman.kotshi

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.tags.TypeAliasTag

fun TypeName.rawType(): ClassName = when (this) {
    is ClassName -> this
    Dynamic -> throw IllegalArgumentException("Dynamic does not have a raw type")
    is LambdaTypeName -> {
        var count = parameters.size
        if (receiver != null) {
            count++
        }
        val functionSimpleName = if (count >= 23) {
            "FunctionN"
        } else {
            "Function$count"
        }
        ClassName("kotlin.jvm.functions", functionSimpleName)
    }

    is TypeVariableName -> throw IllegalArgumentException("Type variable names does not have a raw type")
    is WildcardTypeName -> outTypes.first().rawType()
    is ParameterizedTypeName -> rawType
}

inline fun TypeName.mapTypeArguments(mapper: (TypeName) -> TypeName): TypeName = when (this) {
    is ClassName -> this
    Dynamic -> this
    is LambdaTypeName -> this
    is TypeVariableName -> this
    is WildcardTypeName -> this
    is ParameterizedTypeName -> copy(typeArguments = typeArguments.map(mapper))
}

fun TypeName.unwrapTypeAlias(): TypeName =
    tag<TypeAliasTag>()
        ?.abbreviatedType
        ?.unwrapTypeAlias()
        ?: when (this) {
            is ClassName ->
                tag<TypeAliasTag>()
                    ?.abbreviatedType
                    ?.let { unwrappedType ->
                        var isAnyNullable = isNullable
                        val runningAnnotations = LinkedHashSet(annotations)
                        val nestedUnwrappedType = unwrappedType.unwrapTypeAlias()
                        runningAnnotations.addAll(nestedUnwrappedType.annotations)
                        isAnyNullable = isAnyNullable || nestedUnwrappedType.isNullable
                        nestedUnwrappedType.copy(nullable = isAnyNullable, annotations = runningAnnotations.toList())
                    }
                    ?: this

            is ParameterizedTypeName -> (rawType.unwrapTypeAlias() as ClassName)
                .parameterizedBy(typeArguments.map(TypeName::unwrapTypeAlias))
                .copy(nullable = isNullable, annotations = annotations, tags = tags)

            is TypeVariableName -> TypeVariableName(
                name = name,
                bounds = bounds.map { (TypeName::unwrapTypeAlias)(it) },
                variance = variance
            ).copy(nullable = isNullable, annotations = annotations, tags = tags)

            is WildcardTypeName -> when {
                this == STAR -> this
                outTypes.isNotEmpty() && inTypes.isEmpty() ->
                    WildcardTypeName.producerOf(outTypes[0].unwrapTypeAlias())
                        .copy(nullable = isNullable, annotations = annotations)

                inTypes.isNotEmpty() ->
                    WildcardTypeName.consumerOf(inTypes[0].unwrapTypeAlias())
                        .copy(nullable = isNullable, annotations = annotations)

                else -> throw AssertionError("")
            }

            is LambdaTypeName -> {
                LambdaTypeName.get(
                    receiver = receiver?.unwrapTypeAlias(),
                    parameters = parameters.map {
                        it.toBuilder(type = it.type.unwrapTypeAlias()).build()
                    },
                    returnType = returnType.unwrapTypeAlias(),
                )
                copy(
                    nullable = isNullable,
                    annotations = annotations,
                )
            }

            else -> throw UnsupportedOperationException("Type '${javaClass.name}' is invalid. Only classes, parameterized types, wildcard types, or type variables are allowed.")
        }

fun TypeName.withoutVariance(): TypeName =
    when (this) {
        is ClassName -> this
        Dynamic -> this
        is LambdaTypeName -> this
        is ParameterizedTypeName -> withoutVariance()
        is TypeVariableName -> withoutVariance()
        is WildcardTypeName -> withoutVariance()
    }

fun TypeName.unwrapTypeVariables(): TypeName =
    when (this) {
        is ClassName -> this
        Dynamic -> this
        is LambdaTypeName -> this
        is ParameterizedTypeName -> copy(typeArguments = typeArguments.map { it.unwrapTypeVariables() })
        is TypeVariableName -> {
            val unwrapped = bounds.single().unwrapTypeVariables()
            when (variance) {
                KModifier.IN -> WildcardTypeName.consumerOf(unwrapped)
                else -> WildcardTypeName.producerOf(unwrapped)
            }
        }

        is WildcardTypeName -> if (inTypes.isNotEmpty()) {
            WildcardTypeName.consumerOf(inTypes.single().unwrapTypeVariables())
        } else {
            WildcardTypeName.producerOf(outTypes.single().unwrapTypeVariables())
        }
    }

fun ParameterizedTypeName.withoutVariance(): ParameterizedTypeName =
    copy(typeArguments = typeArguments.map { it.withoutVariance() })

fun TypeVariableName.withoutVariance(): TypeVariableName = TypeVariableName(name, bounds.map { it.withoutVariance() })
fun WildcardTypeName.withoutVariance(): TypeName =
    if (this == STAR) STAR else inTypes.getOrNull(0)?.withoutVariance() ?: outTypes[0]