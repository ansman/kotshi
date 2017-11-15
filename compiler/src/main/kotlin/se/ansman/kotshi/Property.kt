package se.ansman.kotshi

import com.squareup.javapoet.TypeName
import com.squareup.moshi.Json
import com.squareup.moshi.JsonQualifier
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.VariableElement

data class Property(
        private val globalConfig: GlobalConfig,
        private val enclosingClass: Element,
        private val parameter: VariableElement,
        val field: VariableElement,
        val getter: ExecutableElement?
) {
    val adapterKey: AdapterKey by lazy(LazyThreadSafetyMode.NONE) { AdapterKey(type, jsonQualifiers) }

    private val jsonQualifiers: List<Element> by lazy {
        parameter.getJsonQualifiers().let { if (it.isEmpty()) field.getJsonQualifiers() else it }
    }

    val name: CharSequence
        get() = this.field.simpleName

    val jsonName: CharSequence by lazy {
        field.getAnnotation(Json::class.java)?.name
                ?: parameter.getAnnotation(Json::class.java)?.name
                ?: name
    }

    val isNullable: Boolean by lazy {
        field.annotationMirrors.any { it.annotationType.asElement().simpleName.contentEquals("Nullable") }
    }

    val type: TypeName = TypeName.get(field.asType())

    val shouldUseAdapter: Boolean by lazy { !(type.isPrimitive || type.isBoxedPrimitive) || useAdaptersForPrimitives }

    private val useAdaptersForPrimitives: Boolean
        get() = when (enclosingClass.getAnnotation(JsonSerializable::class.java).useAdaptersForPrimitives) {
            PrimitiveAdapters.DEFAULT -> globalConfig.useAdaptersForPrimitives
            PrimitiveAdapters.ENABLED -> true
            PrimitiveAdapters.DISABLED -> false
        }

    private fun Element.getJsonQualifiers(): List<Element> = annotationMirrors
            .asSequence()
            .map { it.annotationType.asElement() }
            .filter { it.getAnnotation(JsonQualifier::class.java) != null }
            .toList()
}