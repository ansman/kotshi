package se.ansman.kotshi.model

import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import se.ansman.kotshi.PrimitiveAdapters
import se.ansman.kotshi.isPrimitive
import se.ansman.kotshi.notNull
import se.ansman.kotshi.unwrapTypeAlias

data class Property(
    val name: String,
    val type: TypeName,
    val jsonName: String,
    val adapterKey: AdapterKey,
    val hasDefaultValue: Boolean,
    val shouldUseAdapter: Boolean,
    val isTransient: Boolean
) {
    init {
        if (isTransient) {
            require(hasDefaultValue)
        }
    }

    companion object {
        fun create(
            name: String,
            type: TypeName,
            jsonQualifiers: Collection<AnnotationModel>,
            globalConfig: GlobalConfig,
            useAdaptersForPrimitives: PrimitiveAdapters,
            parameterJsonName: String?,
            propertyJsonName: String?,
            isTransient: Boolean,
            hasDefaultValue: Boolean,
        ): Property =
            Property(
                name = name,
                type = type,
                adapterKey = AdapterKey(
                    type = type.unwrapTypeAlias().copy(nullable = false, annotations = emptyList()),
                    jsonQualifiers = jsonQualifiers.toSet(),
                ),
                jsonName = parameterJsonName ?: propertyJsonName ?: name,
                isTransient = isTransient,
                hasDefaultValue = hasDefaultValue,
                shouldUseAdapter = jsonQualifiers.isNotEmpty() ||
                    !type.notNull().isPrimitive && type.notNull() != STRING ||
                    when (useAdaptersForPrimitives) {
                        PrimitiveAdapters.DEFAULT -> globalConfig.useAdaptersForPrimitives
                        PrimitiveAdapters.ENABLED -> true
                        PrimitiveAdapters.DISABLED -> false
                    }
            )
    }
}