package se.ansman.kotshi.model

import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.ARRAY
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.DelicateKotlinPoetApi
import com.squareup.kotlinpoet.Dynamic
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.tag
import com.squareup.kotlinpoet.tags.TypeAliasTag
import com.squareup.moshi.Types
import se.ansman.kotshi.notNull
import java.util.Locale

data class AdapterKey(
    val type: TypeName,
    val jsonQualifiers: Set<AnnotationModel>
)

fun AdapterKey.asRuntimeType(typeVariableAccessor: (TypeVariableName) -> CodeBlock): CodeBlock =
    (type.tag<TypeAliasTag>()?.abbreviatedType ?: type).asRuntimeType(typeVariableAccessor)

val AdapterKey.suggestedAdapterName: String
    get() = buildString {
        jsonQualifiers.joinTo(this, "") { it.annotationName.simpleName }
        append(type.baseAdapterName)
        append("Adapter")
        replace(0, 1, this[0].lowercase(Locale.ROOT))
    }

private val TypeName.baseAdapterName: String
    get() = when (this) {
        is ClassName -> simpleNames.joinToString("")
        Dynamic -> "dynamic"
        is LambdaTypeName -> buildString {
            append(receiver?.baseAdapterName ?: "")
            for (parameter in parameters) {
                append(parameter.type.baseAdapterName)
            }
            append(returnType.baseAdapterName)
            append("Lambda")
        }
        is ParameterizedTypeName -> typeArguments.joinToString("") { it.baseAdapterName } + rawType.baseAdapterName
        is TypeVariableName -> name
        is WildcardTypeName -> when {
            inTypes.size == 1 -> inTypes[0].baseAdapterName
            outTypes == STAR.outTypes -> "Star"
            else -> outTypes[0].baseAdapterName
        }
    }

private fun TypeName.asRuntimeType(typeVariableAccessor: (TypeVariableName) -> CodeBlock): CodeBlock =
    when (this) {
        is ParameterizedTypeName ->
            CodeBlock.builder()
                .add(
                    "%T.newParameterizedType(%T::class.javaObjectType", moshiTypes, if (rawType == ARRAY) {
                        // Arrays are special, you cannot just do Array::class.java
                        this
                    } else {
                        rawType.notNull()
                    }
                )
                .apply {
                    for (typeArgument in typeArguments) {
                        add(", ")
                        add(typeArgument.asRuntimeType(typeVariableAccessor))
                    }
                }
                .add(")")
                .build()
        is WildcardTypeName -> when {
            inTypes.size == 1 -> inTypes[0].asRuntimeType(typeVariableAccessor)
            outTypes[0] == ANY -> ANY.asRuntimeType(typeVariableAccessor)
            else -> outTypes[0].asRuntimeType(typeVariableAccessor)
        }
        is LambdaTypeName -> asParameterizedTypeName().asRuntimeType(typeVariableAccessor)
        is TypeVariableName -> typeVariableAccessor(this)
        else -> CodeBlock.of("%T::class.javaObjectType", notNull())
    }

@OptIn(DelicateKotlinPoetApi::class)
val moshiTypes = Types::class.java.asClassName()

private fun LambdaTypeName.asParameterizedTypeName(): ParameterizedTypeName {
    val parameters = mutableListOf<TypeName>()
    receiver?.let(parameters::add)
    parameters.addAll(parameters)
    parameters.add(returnType)
    return ClassName("kotlin", "Function${parameters.size + (if (receiver == null) 0 else 1)}")
        .parameterizedBy(parameters)
}