package se.ansman.kotshi.model

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeVariableName

sealed class GeneratableJsonAdapter {
    abstract val targetPackageName: String
    abstract val targetSimpleNames: List<String>

    open val targetTypeVariables: List<TypeVariableName> get() = emptyList()

    val rawTargetType by lazy {
        ClassName(targetPackageName, targetSimpleNames)
    }

    val targetType: TypeName by lazy {
        if (targetTypeVariables.isEmpty()) {
            rawTargetType
        } else {
            rawTargetType.parameterizedBy(targetTypeVariables)
        }
    }

    val adapterClassName by lazy {
        ClassName(targetPackageName, "Kotshi${targetSimpleNames.joinToString("_")}JsonAdapter")
    }

    val adapterTypeName by lazy {
        val variables = targetTypeVariables
        if (variables.isEmpty()) {
            adapterClassName
        } else {
            adapterClassName.parameterizedBy(variables)
        }
    }

    val adapterName: String get() = adapterClassName.simpleName
}