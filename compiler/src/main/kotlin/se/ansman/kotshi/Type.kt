package se.ansman.kotshi

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.WildcardTypeName
import me.eugeniomarletti.kotlin.metadata.shadow.metadata.ProtoBuf
import me.eugeniomarletti.kotlin.metadata.shadow.metadata.deserialization.NameResolver

internal fun ProtoBuf.Type.asTypeName(
    nameResolver: NameResolver,
    getTypeParameter: (index: Int) -> ProtoBuf.TypeParameter,
    useAbbreviatedType: Boolean = true
): TypeName {

    val argumentList = when {
        useAbbreviatedType && hasAbbreviatedType() -> abbreviatedType.argumentList
        else -> argumentList
    }

    if (hasFlexibleUpperBound()) {
        return WildcardTypeName.producerOf(flexibleUpperBound.asTypeName(nameResolver, getTypeParameter, useAbbreviatedType))
            .runIf(nullable) { copy(nullable = true) }
    } else if (hasOuterType()) {
        return WildcardTypeName.consumerOf(outerType.asTypeName(nameResolver, getTypeParameter, useAbbreviatedType))
            .runIf(nullable) { copy(nullable = true) }
    }

    val realType = when {
        hasTypeParameter() -> return getTypeParameter(typeParameter)
            .asTypeName(nameResolver, getTypeParameter, useAbbreviatedType)
            .runIf(nullable) { copy(nullable = true) }
        hasTypeParameterName() -> typeParameterName
        useAbbreviatedType && hasAbbreviatedType() -> abbreviatedType.typeAliasName
        else -> className
    }

    var typeName: TypeName = ClassName.bestGuess(nameResolver.getString(realType)
        .replace("/", "."))

    if (argumentList.isNotEmpty()) {
        val remappedArgs: Array<TypeName> = argumentList.map { argumentType ->
            val nullableProjection = if (argumentType.hasProjection()) {
                argumentType.projection
            } else null
            if (argumentType.hasType()) {
                argumentType.type.asTypeName(nameResolver, getTypeParameter, useAbbreviatedType)
                    .let { argumentTypeName ->
                        nullableProjection?.let { projection ->
                            when (projection) {
                                ProtoBuf.Type.Argument.Projection.IN -> WildcardTypeName.consumerOf(argumentTypeName)
                                ProtoBuf.Type.Argument.Projection.OUT -> WildcardTypeName.producerOf(argumentTypeName)
                                ProtoBuf.Type.Argument.Projection.STAR -> STAR
                                ProtoBuf.Type.Argument.Projection.INV -> throw UnsupportedOperationException("INV projection is unsupported")
                            }
                        } ?: argumentTypeName
                    }
            } else {
                STAR
            }
        }.toTypedArray()
        typeName = (typeName as ClassName).parameterizedBy(*remappedArgs)
    }

    return typeName.runIf(nullable) { copy(nullable = true) }
}
