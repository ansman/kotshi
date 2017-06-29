package se.ansman.kotshi

import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeVariableName
import com.squareup.moshi.Json
import com.squareup.moshi.JsonQualifier
import com.squareup.moshi.Types
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.VariableElement

data class Property(
        val parameter: VariableElement,
        val field: VariableElement,
        val getter: ExecutableElement?
) {
    val adapterFieldName: String by lazy { "${name}Adapter" }

    val jsonQualifiers: List<Element> by lazy {
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

    fun asRuntimeType(): CodeBlock = type.asRuntimeType()

    private fun TypeName.asRuntimeType(): CodeBlock = when (this) {
        is ParameterizedTypeName -> when (rawType.reflectionName()) {
            else -> CodeBlock.builder()
                    .add("\$T.newParameterizedType(\$T.class", Types::class.java, rawType.box())
                    .apply {
                        for (typeArgument in typeArguments) {
                            add(", ")
                            add(typeArgument.asRuntimeType())
                        }
                    }
                    .add(")")
                    .build()
        }
        else -> CodeBlock.of("\$T.class", box())
    }

    val isGeneric: Boolean by lazy { type is TypeVariableName }

    private fun Element.getJsonQualifiers(): List<Element> = annotationMirrors
            .asSequence()
            .map { it.annotationType.asElement() }
            .filter { it.getAnnotation(JsonQualifier::class.java) != null }
            .toList()
}