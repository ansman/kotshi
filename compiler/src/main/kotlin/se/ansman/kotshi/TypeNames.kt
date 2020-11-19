package se.ansman.kotshi

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.metadata.specs.TypeNameAliasTag
import com.squareup.kotlinpoet.tag

fun TypeName.unwrapTypeAlias(): TypeName =
    when (this) {
        is ClassName ->
            tag<TypeNameAliasTag>()
                ?.type
                ?.let { unwrappedType ->
                    var isAnyNullable = isNullable
                    val runningAnnotations = LinkedHashSet(annotations)
                    val nestedUnwrappedType = unwrappedType.unwrapTypeAlias()
                    runningAnnotations.addAll(nestedUnwrappedType.annotations)
                    isAnyNullable = isAnyNullable || nestedUnwrappedType.isNullable
                    nestedUnwrappedType.copy(nullable = isAnyNullable, annotations = runningAnnotations.toList())
                }
                ?: this
        is ParameterizedTypeName -> rawType
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

