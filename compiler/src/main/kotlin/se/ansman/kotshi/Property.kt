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
    val adapterKey: AdapterKey = AdapterKey(type, jsonQualifiers)

    private val jsonQualifiers: List<Element> =
            parameter.getJsonQualifiers().let { if (it.isEmpty()) field.getJsonQualifiers() else it }

    val name: CharSequence = field.simpleName

    val jsonName: CharSequence = field.getAnnotation(Json::class.java)?.name
                ?: parameter.getAnnotation(Json::class.java)?.name
                ?: name

    val isNullable: Boolean =
        field.annotationMirrors.any { it.annotationType.asElement().simpleName.contentEquals("Nullable") }

    val type: TypeName = TypeName.get(field.asType())

    private val useAdaptersForPrimitives: Boolean =
            when (enclosingClass.getAnnotation(JsonSerializable::class.java).useAdaptersForPrimitives) {
                PrimitiveAdapters.DEFAULT -> globalConfig.useAdaptersForPrimitives
                PrimitiveAdapters.ENABLED -> true
                PrimitiveAdapters.DISABLED -> false
            }

    val shouldUseAdapter: Boolean = !(type.isPrimitive || type.isBoxedPrimitive) || useAdaptersForPrimitives

    private fun Element.getJsonQualifiers(): List<Element> = annotationMirrors
            .asSequence()
            .map { it.annotationType.asElement() }
            .filter { it.getAnnotation(JsonQualifier::class.java) != null }
            .toList()
}