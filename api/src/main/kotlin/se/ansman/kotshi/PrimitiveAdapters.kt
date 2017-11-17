package se.ansman.kotshi

/**
 * Flag for enabling/disabling the use of adapters for primitive types.
 */
enum class PrimitiveAdapters {
    /**
     * Follow the module default ([KotshiJsonAdapterFactory]).
     */
    DEFAULT,

    /**
     * Use adapters for primitive types (including boxed/nullable primitives).
     */
    ENABLED,

    /**
     * Don't use adapters for primitive types (including boxed/nullable primitives).
     */
    DISABLED
}