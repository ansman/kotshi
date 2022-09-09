@file:OptIn(ExperimentalKotshiApi::class)

package se.ansman.kotshi

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import se.ansman.kotshi.ManuallyRegisteredGenericAdapter.GenericType
import java.lang.reflect.Type

@RegisterJsonAdapter
object ManuallyRegisteredAdapter : JsonAdapter<ManuallyRegisteredAdapter.Type>() {
    override fun fromJson(reader: JsonReader): Type = throw UnsupportedOperationException()
    override fun toJson(writer: JsonWriter, value: Type?) = throw UnsupportedOperationException()

    object Type
}

@RegisterJsonAdapter
object OverwrittenAdapter : JsonAdapter<OverwrittenAdapter.Data<String>>() {
    override fun fromJson(reader: JsonReader): Data<String> = throw UnsupportedOperationException()
    override fun toJson(writer: JsonWriter, value: Data<String>?) = throw UnsupportedOperationException()

    @JsonSerializable
    data class Data<T>(val type: T)
}

@RegisterJsonAdapter
@Hello
@QualifierWithDefaults(string = "Goodbye")
object QualifiedAdapter : JsonAdapter<String>() {
    override fun fromJson(reader: JsonReader): String = throw UnsupportedOperationException()
    override fun toJson(writer: JsonWriter, value: String?) = throw UnsupportedOperationException()
}

abstract class AbstractAdapter<T> : JsonAdapter<List<T>>() {
    override fun fromJson(reader: JsonReader): List<T>? = throw UnsupportedOperationException()

    override fun toJson(writer: JsonWriter, value: List<T>?) = throw UnsupportedOperationException()

    @RegisterJsonAdapter
    object RealAdapter : AbstractAdapter<RealAdapter.Data<String>>() {
        @JsonSerializable
        data class Data<T>(val type: T)
    }

    @RegisterJsonAdapter
    class GenericAdapter<T> : AbstractAdapter<GenericAdapter.Data<T>>() {
        @JsonSerializable
        data class Data<T>(val type: T)
    }
}

@RegisterJsonAdapter
class ManuallyRegisteredGenericAdapter<T : Number>(val types: Array<Type>, val moshi: Moshi) :
    JsonAdapter<GenericType<T>>() {
    override fun fromJson(reader: JsonReader): GenericType<T> = throw UnsupportedOperationException()
    override fun toJson(writer: JsonWriter, value: GenericType<T>?) = throw UnsupportedOperationException()

    @Suppress("unused")
    class GenericType<T>
}

@OptIn(InternalKotshiApi::class)
@RegisterJsonAdapter
class ManuallyRegisteredWrappedGenericAdapter :
    NamedJsonAdapter<ManuallyRegisteredWrappedGenericAdapter.GenericType<List<String>>>("Test") {
    override fun fromJson(reader: JsonReader): GenericType<List<String>> = throw UnsupportedOperationException()
    override fun toJson(writer: JsonWriter, value: GenericType<List<String>>?) = throw UnsupportedOperationException()

    @Suppress("unused")
    class GenericType<T>
}

abstract class PrioritizedAdapter<T> : JsonAdapter<T>() {
    override fun fromJson(reader: JsonReader): T? = throw UnsupportedOperationException()
    override fun toJson(writer: JsonWriter, value: T?) = throw UnsupportedOperationException()

    @JsonSerializable
    data class Data<T>(val value: T)

    @RegisterJsonAdapter(priority = 1)
    object CStringAdapter : PrioritizedAdapter<Data<String>>()

    @RegisterJsonAdapter
    class BGenericAdapter<T> : PrioritizedAdapter<Data<T>>()

    @RegisterJsonAdapter(priority = -1)
    object AIntAdapter : PrioritizedAdapter<Data<Int>>()
}