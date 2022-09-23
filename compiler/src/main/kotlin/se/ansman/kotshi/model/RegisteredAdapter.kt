package se.ansman.kotshi.model

import com.squareup.kotlinpoet.*
import se.ansman.kotshi.KotshiConstructor

internal data class RegisteredAdapter(
    val adapterTypeName: TypeName,
    val targetType: TypeName,
    val constructor: KotshiConstructor?,
    val qualifiers: Set<AnnotationModel>,
    val priority: Int,
) : Comparable<RegisteredAdapter> {
    private val sortKey = adapterTypeName.toString()

    val adapterClassName = when (adapterTypeName) {
        is ClassName -> adapterTypeName
        is ParameterizedTypeName -> adapterTypeName.rawType
        else -> throw IllegalArgumentException("Unknown adapter typename type: $adapterTypeName")
    }

    val requiresDeepTypeCheck: Boolean = when (targetType) {
        is ClassName -> false
        Dynamic -> false
        is LambdaTypeName -> true
        is ParameterizedTypeName -> true
        is TypeVariableName -> true
        is WildcardTypeName -> true
    }

    override fun compareTo(other: RegisteredAdapter): Int {
        if (priority != other.priority) {
            return other.priority.compareTo(priority)
        }
        return sortKey.compareTo(other.sortKey)
    }
}
