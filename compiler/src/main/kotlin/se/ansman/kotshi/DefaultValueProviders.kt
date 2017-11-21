package se.ansman.kotshi

import com.squareup.javapoet.ParameterizedTypeName
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeVariable
import javax.lang.model.util.Types

class DefaultValueProviders(private val types: Types) {
    private val providers: MutableList<DefaultValueProvider> = ArrayList()

    fun register(provider: DefaultValueProvider) {
        providers.add(provider)
    }

    fun validate() {
        providers
                .asSequence()
                .groupByTo(LinkedHashMap()) { it.type to it.qualifiers }
                .forEach { (type, qualifiers), grouped ->
                    // This fix is needed because @JvmStatic causes a static function AND the companion function to be
                    // generated so we remove the companion function
                    if (grouped.size > 1) {
                        val companion = grouped.firstOrNull {
                            it.element.enclosingElement.simpleName.contentEquals("Companion")
                        }
                        if (companion != null) {
                            grouped.remove(companion)
                            providers.remove(companion)
                        }
                    }

                    if (grouped.size > 1) {
                        throw ProcessingError("Multiple provides provide values for type $type with qualifiers $qualifiers: ${grouped.map { it.accessor }}", grouped.first().element)
                    }
                }
    }

    operator fun get(property: Property): DefaultValueProvider =
            get(property, true)
                    ?: get(property, false)
                    ?: throw ProcessingError("No default value provider found", property.parameter)

    private fun get(property: Property, onlyExactMatch: Boolean): DefaultValueProvider? {
        val applicable = providers
                .asSequence()
                .filter { property.defaultValueQualifiers == it.qualifiers }
                .filter {
                    val rawProviderType = types.erasure(it.typeMirror)
                    val rawPropertyType = property.rawTypeMirror
                    // The erased types must match or there is no match
                    if (onlyExactMatch) {
                        if (!types.isSameType(rawProviderType, rawPropertyType)) {
                            return@filter false
                        }
                    } else {
                        if (!types.isAssignable(rawProviderType, rawPropertyType)) {
                            return@filter false
                        }
                    }

                    // If both types are parameterized we need to check the bounds too
                    if (it.type is ParameterizedTypeName && property.type is ParameterizedTypeName) {
                        val prov = it.typeMirror as DeclaredType
                        val prop = property.typeMirror as DeclaredType

                        // Then all type arguments must match
                        prop.typeArguments.zip(prov.typeArguments) { propTypeArg, provTypeArg ->
                            if (types.isAssignable(propTypeArg, provTypeArg)) {
                                return@zip
                            }
                            if (!onlyExactMatch && provTypeArg is TypeVariable) {
                                if (!types.isAssignable(propTypeArg, provTypeArg.upperBound)) {
                                    return@filter false
                                }
                            } else {
                                return@filter false
                            }
                        }
                    }
                    return@filter true
                }
                .toList()

        return when (applicable.size) {
            0 -> null
            1 -> applicable.first()
            else ->
                throw ProcessingError("Multiple providers matches: ${applicable.map { it.accessor }}", property.field)
        }
    }
}