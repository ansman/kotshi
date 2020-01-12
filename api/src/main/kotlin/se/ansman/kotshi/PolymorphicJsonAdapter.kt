package se.ansman.kotshi

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter

internal class PolymorphicJsonAdapter<T : Any, S : Any>(
    val typeName: String,
    val typeAdapter: JsonAdapter<T>,
    val adapters: Map<T, JsonAdapter<out S>>,
    val defaultType: T? = null
) : JsonAdapter<S>() {
    private val options = JsonReader.Options.of(typeName)

    init {
        if (defaultType != null) {
            require(defaultType in adapters) {
                "Default type specified but was not present in the adapters"
            }
        }
    }

    override fun fromJson(reader: JsonReader): S? {
        val peeked = reader.peekJson()
        peeked.beginObject()
        peeked.setFailOnUnknown(false)
        while (peeked.hasNext()) {
            when (peeked.selectName(options)) {
                0 -> {
                    val type = typeAdapter.fromJson(peeked)
                    val adapter = adapters[type]
                        ?: adapters[defaultType]
                        ?: throw JsonDataException("Unknown type. Expected one of ${adapters.keys} but was $type")
                    return adapter.fromJson(reader)
                }
                else -> {
                    peeked.skipName()
                    peeked.skipValue()
                }
            }
        }
        throw JsonDataException("Could not find type field $typeName in JSON")
    }

    override fun toJson(writer: JsonWriter, value: S?) {

    }
}

