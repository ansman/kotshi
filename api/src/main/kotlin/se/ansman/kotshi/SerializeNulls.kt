package se.ansman.kotshi

import com.squareup.moshi.JsonWriter

/**
 * Flag for enabling/disabling [null serialization][JsonWriter.serializeNulls]. Can be applied both to a factory or to
 * individual adapters.
 */
enum class SerializeNulls {
    /**
     * Don't change what the reader has set.
     */
    DEFAULT,

    /**
     * Enable null serialization for this adapter and for child adapters (unless they override it).
     */
    ENABLED,

    /**
     * Disable null serialization for this adapter and for child adapters (unless they override it).
     */
    DISABLED
}