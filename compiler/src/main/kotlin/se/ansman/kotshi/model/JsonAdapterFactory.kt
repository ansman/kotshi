package se.ansman.kotshi.model

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName

data class JsonAdapterFactory(
    val targetType: ClassName,
    val usageType: UsageType,
    val adapters: List<GeneratedAdapter>,
) {
    val factoryClassName: ClassName = ClassName(targetType.packageName, "Kotshi${targetType.simpleNames.joinToString("_")}")

    sealed class UsageType {
        /** Generates an object that directly implements JsonAdapter.Factory */
        object Standalone : UsageType()
        /** Generates an object that implements the given [parent] which in turn implements JsonAdapter.Factory */
        data class Subclass(val parent: TypeName) : UsageType()
    }
}