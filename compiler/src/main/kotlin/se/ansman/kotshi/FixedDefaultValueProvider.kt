package se.ansman.kotshi

import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.TypeName

class FixedDefaultValueProvider(
        override val type: TypeName,
        override val accessor: CodeBlock
) : DefaultValueProvider {
    override val canReturnNull: Boolean
        get() = false

    override val isNullable: Boolean
        get() = false

    override val isStatic: Boolean
        get() = true
}