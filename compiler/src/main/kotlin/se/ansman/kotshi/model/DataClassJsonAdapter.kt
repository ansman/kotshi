package se.ansman.kotshi.model

import com.squareup.kotlinpoet.TypeVariableName
import se.ansman.kotshi.SerializeNulls

data class DataClassJsonAdapter(
    override val targetPackageName: String,
    override val targetSimpleNames: List<String>,
    override val targetTypeVariables: List<TypeVariableName>,
    val polymorphicLabels: Map<String, String>,
    val properties: List<Property>,
    val serializeNulls: SerializeNulls,
    val constructorSignature: String,
) : GeneratableJsonAdapter() {
    val serializedProperties = properties.filterNot { it.isIgnored }
}