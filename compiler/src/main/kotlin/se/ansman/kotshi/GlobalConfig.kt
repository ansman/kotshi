package se.ansman.kotshi

data class GlobalConfig(
    val useAdaptersForPrimitives: Boolean,
    val serializeNulls: SerializeNulls
) {
    constructor(factory: KotshiJsonAdapterFactory) : this(
        useAdaptersForPrimitives = factory.useAdaptersForPrimitives,
        serializeNulls = factory.serializeNulls
    )

    companion object {
        val DEFAULT = GlobalConfig(
            useAdaptersForPrimitives = false,
            serializeNulls = SerializeNulls.DEFAULT
        )
    }
}