package se.ansman.kotshi

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.moshi.Json
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Types

class Property(
        defaultValueProviders: DefaultValueProviders,
        types: Types,
        globalConfig: GlobalConfig,
        enclosingClass: Element,
        val parameter: VariableElement,
        val field: VariableElement?,
        val getter: ExecutableElement?
) {
    val typeMirror: TypeMirror = field?.asType() ?: parameter.asType()

    val rawTypeMirror: TypeMirror by lazy { types.erasure(typeMirror) }

    val type: TypeName = typeMirror.asTypeName()

    val defaultValueQualifier = parameter.getDefaultValueQualifier()

    val adapterKey: AdapterKey = AdapterKey(type, parameter.getJsonQualifiers())

    val name: CharSequence = field?.simpleName ?: parameter.simpleName

    val jsonName: CharSequence = field?.getAnnotation(Json::class.java)?.name
            ?: parameter.getAnnotation(Json::class.java)?.name
            ?: name

    val isNullable: Boolean = parameter.hasAnnotation("Nullable")

    private val useAdaptersForPrimitives: Boolean =
            when (enclosingClass.getAnnotation(JsonSerializable::class.java).useAdaptersForPrimitives) {
                PrimitiveAdapters.DEFAULT -> globalConfig.useAdaptersForPrimitives
                PrimitiveAdapters.ENABLED -> true
                PrimitiveAdapters.DISABLED -> false
            }

    val shouldUseAdapter: Boolean = useAdaptersForPrimitives ||
            adapterKey.jsonQualifiers.isNotEmpty() ||
            !(type.isPrimitive || type.isBoxedPrimitive || type == TYPE_NAME_STRING)

    val defaultValueProvider: DefaultValueProvider?

    init {
        require(getter != null || field != null)

        defaultValueProvider = if (defaultValueQualifier != null || parameter.hasAnnotation<JsonDefaultValue>()) {
            if (adapterKey.isGeneric) {
                throw ProcessingError("You cannot use default values on a generic type", parameter)
            }
            (type as? ParameterizedTypeName)?.typeArguments?.forEach {
                if (it !is ClassName) {
                    throw ProcessingError("Generic classes must not have wildcard types if you want to use default values", parameter)
                }
            }
            defaultValueProviders[this]
        } else {
            null
        }

    }
}