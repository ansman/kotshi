package se.ansman.kotshi.model

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.Dynamic
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.WildcardTypeName
import se.ansman.kotshi.KotshiConstructor

internal data class RegisteredAdapter<out OE>(
    val adapterTypeName: TypeName,
    val targetType: TypeName,
    val constructor: KotshiConstructor?,
    val qualifiers: Set<AnnotationModel>,
    val priority: Int,
    val originatingElement: OE,
) : Comparable<RegisteredAdapter<*>> {
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

    override fun compareTo(other: RegisteredAdapter<*>): Int {
        if (priority != other.priority) {
            return other.priority.compareTo(priority)
        }
        return sortKey.compareTo(other.sortKey)
    }
}
