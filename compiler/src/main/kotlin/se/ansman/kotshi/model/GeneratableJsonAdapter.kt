package se.ansman.kotshi.model

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeVariableName

sealed class GeneratableJsonAdapter {
    abstract val targetPackageName: String
    abstract val targetSimpleNames: List<String>

    open val targetTypeVariables: List<TypeVariableName> get() = emptyList()

    val targetType: TypeName by lazy {
        val rawType = ClassName(targetPackageName, targetSimpleNames)
        if (targetTypeVariables.isEmpty()) {
            rawType
        } else {
            rawType.parameterizedBy(targetTypeVariables)
        }
    }

    val adapterClassName by lazy {
        ClassName(targetPackageName, "Kotshi${targetSimpleNames.joinToString("_")}JsonAdapter")
    }

    val adapterName: String get() = adapterClassName.simpleName
}