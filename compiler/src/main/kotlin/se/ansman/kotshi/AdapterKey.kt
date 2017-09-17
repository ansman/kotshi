package se.ansman.kotshi

import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeVariableName
import com.squareup.moshi.Types
import javax.lang.model.element.Element

data class AdapterKey(val type: TypeName,
                      val jsonQualifiers: List<Element>) {

    val isGeneric: Boolean
        get() = type is TypeVariableName

    fun asRuntimeType(): CodeBlock = type.asRuntimeType()

    private fun TypeName.asRuntimeType(): CodeBlock = when (this) {
        is ParameterizedTypeName ->
            CodeBlock.builder()
                    .add("\$T.newParameterizedType(\$T.class", Types::class.java, rawType)
                    .apply {
                        for (typeArgument in typeArguments) {
                            add(", ")
                            add(typeArgument.asRuntimeType())
                        }
                    }
                    .add(")")
                    .build()
        else -> CodeBlock.of("\$T.class", this)
    }
}