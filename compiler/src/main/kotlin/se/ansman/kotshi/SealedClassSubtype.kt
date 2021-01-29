package se.ansman.kotshi

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.Dynamic
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.tag
import se.ansman.kotshi.generators.typesParameter
import java.lang.reflect.ParameterizedType
import javax.lang.model.element.TypeElement

class SealedClassSubtype(
    metadataAccessor: MetadataAccessor,
    val type: TypeElement,
    val label: String
) : TypeRenderer() {
    val className = type.asClassName()
    val typeSpec = metadataAccessor.getTypeSpec(type)

    override fun renderTypeVariable(typeVariable: TypeVariableName): CodeBlock {
        val superParameters = (typeSpec.superclass as? ParameterizedTypeName)
            ?.typeArguments
            ?: emptyList()

        fun TypeName.findAccessor(typesIndex: Int): CodeBlock? {
            return when (this) {
                is ClassName,
                Dynamic,
                is LambdaTypeName -> null
                is WildcardTypeName -> {
                    for (outType in outTypes) {
                        outType.findAccessor(typesIndex)?.let { return it }
                    }
                    for (inType in inTypes) {
                        inType.findAccessor(typesIndex)?.let { return it }
                    }
                    null
                }
                is TypeVariableName -> {
                    if (name.contentEquals(typeVariable.name)) {
                        CodeBlock.of("")
                    } else {
                        for (bound in bounds) {
                            bound.findAccessor(typesIndex)?.let { return it }
                        }
                        null
                    }
                }
                is ParameterizedTypeName -> {
                    typeArguments.forEachIndexed { index, typeName ->
                        val accessor = typeName.findAccessor(typesIndex) ?: return@forEachIndexed
                        return CodeBlock.builder()
                            .addControlFlow(".let") {
                                add("it as? %T\n", ParameterizedType::class.java)
                                indent()
                                add("?: throw %T(%P)\n", IllegalArgumentException::class.java, "The type \${${typesParameter.name}[$typesIndex]} is not a valid type constraint for the \$this")
                                unindent()
                            }
                            .add(".actualTypeArguments[%L]", index)
                            .add(accessor)
                            .build()
                    }
                    null
                }
            }
        }

        superParameters.forEachIndexed { index, superParameter ->
            val accessor = superParameter.findAccessor(index) ?: return@forEachIndexed
            return CodeBlock.builder()
                .add("%N[%L]\n", typesParameter, index)
                .indent()
                .add(accessor)
                .unindent()
                .build()
        }
        throw ProcessingError("Could not determine type variable type", typeVariable.tag() ?: type)
    }
}