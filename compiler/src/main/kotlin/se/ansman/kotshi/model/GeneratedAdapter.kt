package se.ansman.kotshi.model

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeSpec
import se.ansman.kotshi.KotshiConstructor
import se.ansman.kotshi.ProguardConfig
import se.ansman.kotshi.renderer.AdapterRenderer


data class GeneratedAdapter(
    val adapter: GeneratableJsonAdapter,
    val fileSpec: FileSpec,
    val proguardConfig: ProguardConfig?
) : Comparable<GeneratedAdapter> {
    val adapterClassName: ClassName = ClassName(adapter.targetPackageName, adapter.adapterName)

    val typeSpec = fileSpec.members.filterIsInstance<TypeSpec>().single()
    internal val constructor = KotshiConstructor(
        moshiParameterName = typeSpec.primaryConstructor
            ?.parameters
            ?.find { it.name == AdapterRenderer.moshiParameterName }
            ?.name,
        typesParameterName = typeSpec.primaryConstructor
            ?.parameters
            ?.find { it.name == AdapterRenderer.typesParameterName }
            ?.name,
    )

    val typesParameter: ParameterSpec?
        get() = typeSpec.primaryConstructor?.parameters?.find { it.name == AdapterRenderer.typesParameterName }

    val moshiParameter: ParameterSpec?
        get() = typeSpec.primaryConstructor?.parameters?.find { it.name == AdapterRenderer.moshiParameterName }

    override fun compareTo(other: GeneratedAdapter): Int = adapterClassName.compareTo(other.adapterClassName)
}
