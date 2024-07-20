package se.ansman.kotshi.model

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.Dynamic
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.moshi.JsonAdapter
import se.ansman.kotshi.KotshiConstructor
import se.ansman.kotshi.Types

internal data class JsonAdapterFactory(
    val targetType: ClassName,
    val generatedAdapters: List<GeneratedAdapter<*>>,
    val manuallyRegisteredAdapters: List<RegisteredAdapter<*>>,
) {
    val isEmpty: Boolean get() = generatedAdapters.isEmpty() && manuallyRegisteredAdapters.isEmpty()
    val factoryClassName: ClassName =
        ClassName(targetType.packageName, "Kotshi${targetType.simpleNames.joinToString("_")}")

    companion object {
        fun <E> E.getManualAdapter(
            logError: (String, E) -> Unit,
            getSuperClass: E.() -> E?,
            getSuperTypeName: E.() -> TypeName?,
            adapterClassName: ClassName,
            typeVariables: E.() -> List<TypeVariableName>,
            isObject: Boolean,
            isAbstract: Boolean,
            priority: Int,
            getKotshiConstructor: E.() -> KotshiConstructor?,
            getJsonQualifiers: E.() -> Set<AnnotationModel>,
        ): RegisteredAdapter<E>? {
            val adapterTypeVariables = typeVariables()
            val adapterType = findJsonAdapterType(
                logError = logError,
                typeVariables = typeVariables,
                getSuperClass = getSuperClass,
                getSuperTypeName = getSuperTypeName,
            ) ?: run {
                logError(
                    "@RegisterJsonAdapter can only be used on classes that extends JsonAdapter.",
                    this
                )
                return null
            }

            if (isAbstract) {
                logError(
                    "@RegisterJsonAdapter cannot be applied to generic classes.",
                    this
                )
                return null
            }

            return RegisteredAdapter(
                adapterTypeName = if (adapterTypeVariables.isEmpty()) {
                    adapterClassName
                } else {
                    adapterClassName.parameterizedBy(adapterTypeVariables)
                },
                targetType = adapterType,
                constructor = if (isObject) {
                    null
                } else {
                    getKotshiConstructor() ?: run {
                        logError(
                            "Could not find a suitable constructor. A constructor can have any combination of a parameter of type Moshi and of a parameter of type Array<Type>.",
                            this
                        )
                        return null
                    }
                },
                qualifiers = getJsonQualifiers(),
                priority = priority,
                originatingElement = this
            )
        }

        private fun <E> E.findJsonAdapterType(
            logError: (String, E) -> Unit,
            typeVariables: E.() -> List<TypeVariableName>,
            getSuperClass: E.() -> E?,
            getSuperTypeName: E.() -> TypeName?,
            typeVariableResolver: (index: Int, TypeVariableName) -> TypeName = { _, type -> type },
        ): TypeName? {
            val typeVariableIndexByName = typeVariables()
                .withIndex()
                .associateBy({ it.value.name }, { it.index })

            fun TypeName.resolve(): TypeName = when (this) {
                is ClassName -> this
                Dynamic -> this
                is LambdaTypeName -> this // TODO: Resolve names here if possible
                is ParameterizedTypeName -> copy(typeArguments = typeArguments.map { it.resolve() })
                is TypeVariableName -> typeVariableResolver(typeVariableIndexByName.getValue(name), this)
                is WildcardTypeName -> this
            }

            val superTypeName = getSuperTypeName()
            val adapterType = superTypeName?.asJsonAdapterTypeOrNull()
                ?: getSuperClass()?.findJsonAdapterType(
                    logError,
                    typeVariables,
                    getSuperClass,
                    getSuperTypeName
                ) { index, _ ->
                    (superTypeName as ParameterizedTypeName).typeArguments[index]
                }

            return adapterType?.resolve()
        }

        private fun TypeName.asJsonAdapterTypeOrNull(): TypeName? =
            (this as? ParameterizedTypeName)
                ?.takeIf { it.rawType.toString() == JsonAdapter::class.java.name }
                ?.typeArguments
                ?.single()
    }
}

internal fun <C, P> Sequence<C>.findKotshiConstructor(
    parameters: C.() -> Iterable<P>,
    type: P.() -> TypeName,
    hasDefaultValue: P.() -> Boolean,
    name: P.() -> String,
): KotshiConstructor? {
    var hasConstructor = false
    outer@ for (constructor in this) {
        hasConstructor = true
        var moshiParameterName: String? = null
        var typesParameterName: String? = null
        for (parameter in constructor.parameters()) {
            when (type(parameter)) {
                Types.Moshi.moshi -> {
                    if (moshiParameterName != null) {
                        continue@outer
                    }
                    moshiParameterName = parameter.name()
                }

                Types.Kotshi.typesArray -> {
                    if (typesParameterName != null) {
                        continue@outer
                    }
                    typesParameterName = parameter.name()
                }

                else -> if (!parameter.hasDefaultValue()) {
                    continue@outer
                }
            }
        }
        return KotshiConstructor(
            moshiParameterName = moshiParameterName,
            typesParameterName = typesParameterName
        )
    }
    // Every class without an explicit constructor has a default constructor
    if (!hasConstructor) {
        return KotshiConstructor()
    }
    return null
}
