package se.ansman.kotshi

import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.TypeName

interface DefaultValueProvider {
    val type: TypeName
    val accessor: CodeBlock
    val canReturnNull: Boolean
    val isNullable: Boolean
    val isStatic: Boolean
}