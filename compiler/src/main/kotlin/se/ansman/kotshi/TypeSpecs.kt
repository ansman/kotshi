package se.ansman.kotshi

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.tag

internal val TypeSpec.typeName: TypeName
    get() = tag<ClassName>()!!
        .let {
            if (typeVariables.isEmpty()) {
                it
            } else {
                it.parameterizedBy(typeVariables)
            }
        }

internal fun TypeSpec.constructors(): Sequence<FunSpec> =
    (primaryConstructor?.let { sequenceOf(it) } ?: emptySequence()) + funSpecs
        .asSequence()
        .filter { it.isConstructor }

internal data class KotshiConstructor(
    val moshiParameterName: String? = null,
    val typesParameterName: String? = null,
)

internal val KotshiConstructor.hasParameters: Boolean
    get() = moshiParameterName != null || typesParameterName != null