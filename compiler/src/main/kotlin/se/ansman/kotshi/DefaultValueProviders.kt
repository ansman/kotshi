package se.ansman.kotshi

import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeVariable
import javax.lang.model.util.Types

class DefaultValueProviders(private val types: Types) {
    private val providers: MutableList<ComplexDefaultValueProvider> = ArrayList()

    fun register(provider: ComplexDefaultValueProvider) {
        providers.add(provider)
    }

    fun validate() {
        providers
            .asSequence()
            .groupByTo(LinkedHashMap()) { it.type to it.qualifier }
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
                    throw ProcessingError("Multiple providers provide values for type $type with qualifier $qualifiers: ${grouped.map { it.accessor }}", grouped.first().element)
                }
            }
    }

    operator fun get(property: Property): DefaultValueProvider {
        return getPrimitiveAnnotation<String, JsonDefaultValueString>(property) { CodeBlock.of("\$S", it.value) }
            ?: getPrimitiveAnnotation<Boolean, JsonDefaultValueBoolean>(property) { CodeBlock.of("${it.value}") }
            ?: getPrimitiveAnnotation<Byte, JsonDefaultValueByte>(property) { CodeBlock.of("${it.value}") }
            ?: getPrimitiveAnnotation<Char, JsonDefaultValueChar>(property) { CodeBlock.of("'${it.value.toString().replace("'", "\\'")}'") }
            ?: getPrimitiveAnnotation<Short, JsonDefaultValueShort>(property) { CodeBlock.of("${it.value}") }
            ?: getPrimitiveAnnotation<Int, JsonDefaultValueInt>(property) { CodeBlock.of("${it.value}") }
            ?: getPrimitiveAnnotation<Long, JsonDefaultValueLong>(property) { CodeBlock.of("${it.value}L") }
            ?: getPrimitiveAnnotation<Float, JsonDefaultValueFloat>(property) { CodeBlock.of("${it.value}f") }
            ?: getPrimitiveAnnotation<Double, JsonDefaultValueDouble>(property) { CodeBlock.of("${it.value}") }
            ?: get(property, true)
            ?: get(property, false)
            ?: throw ProcessingError("No default value provider found", property.parameter)
    }

    private inline fun <reified T, reified A : Annotation> getPrimitiveAnnotation(property: Property, block: (A) -> CodeBlock): DefaultValueProvider? =
        property.parameter.getAnnotation(A::class.java)?.let {
            val type = if (property.type.isPrimitive) property.type.box() else property.type
            if (type == TypeName.get(T::class.java)) {
                FixedDefaultValueProvider(TypeName.get(T::class.java), block(it))
            } else {
                throw ProcessingError("${A::class.java.simpleName} is only applicable to ${T::class.java.simpleName}s", property.parameter)
            }
        }

    private fun get(property: Property, onlyExactMatch: Boolean): ComplexDefaultValueProvider? {
        val applicable = providers
            .asSequence()
            .filter { property.defaultValueQualifier == it.qualifier }
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

                    if (prop.typeArguments.size != prov.typeArguments.size) {
                        return@filter false
                    }

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
                throw ProcessingError("Multiple providers matches: ${applicable.map { it.accessor }}", property.field
                    ?: property.parameter)
        }
    }
}