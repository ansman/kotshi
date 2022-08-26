package se.ansman.kotshi.model

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec

data class GeneratedAdapter(
    val adapter: GeneratableJsonAdapter,
    val fileSpec: FileSpec,
) {
    val adapterClassName: ClassName = ClassName(adapter.targetPackageName, adapter.adapterName)

    val typeSpec = fileSpec.members.filterIsInstance<TypeSpec>().single()
    val requiresTypes: Boolean
        get() = typeSpec.primaryConstructor?.parameters?.any { it.name == "types" } ?: false

    val requiresMoshi: Boolean
        get() = typeSpec.primaryConstructor?.parameters?.any { it.name == "moshi" } ?: false

}
