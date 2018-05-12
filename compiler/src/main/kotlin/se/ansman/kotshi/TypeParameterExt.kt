package se.ansman.kotshi

import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeVariableName
import me.eugeniomarletti.kotlin.metadata.shadow.metadata.ProtoBuf
import me.eugeniomarletti.kotlin.metadata.shadow.metadata.deserialization.NameResolver

internal fun ProtoBuf.TypeParameter.asTypeName(
    nameResolver: NameResolver,
    getTypeParameter: (index: Int) -> ProtoBuf.TypeParameter,
    resolveAliases: Boolean = false
): TypeVariableName {
    val possibleBounds = upperBoundList.map {
        it.asTypeName(nameResolver, getTypeParameter, resolveAliases)
    }
    return if (possibleBounds.isEmpty()) {
        TypeVariableName(
            name = nameResolver.getString(name),
            variance = variance.asKModifier()
        )
    } else {
        TypeVariableName(
            name = nameResolver.getString(name),
            bounds = *possibleBounds.toTypedArray(),
            variance = variance.asKModifier()
        )
    }
}

private fun ProtoBuf.TypeParameter.Variance.asKModifier(): KModifier? =
    when (this) {
        ProtoBuf.TypeParameter.Variance.IN -> KModifier.IN
        ProtoBuf.TypeParameter.Variance.OUT -> KModifier.OUT
        ProtoBuf.TypeParameter.Variance.INV -> null
    }