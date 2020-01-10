package se.ansman.kotshi

import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Types
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Proxy
import java.lang.reflect.Type

/**
 * A helper class for Kotshi.
 *
 * These functions should not be considered public and are subject to change without notice.
 */
object KotshiUtils {
    private const val ERROR_FORMAT = "Expected %s but was %s at path %s"

    @JvmStatic
    val Type.typeArgumentsOrFail: Array<Type>
        get() = (this as? ParameterizedType)?.actualTypeArguments
            ?: throw IllegalArgumentException("""
                    ${Types.getRawType(this).simpleName} is generic and requires you to specify its type
                    arguments. Don't request an adapter using Type::class.java or value.javaClass but rather using
                    Types.newParameterizedType.
                """.trimIndent())

    @JvmStatic
    fun StringBuilder?.appendNullableError(propertyName: String, jsonName: String): StringBuilder =
        if (this == null) {
            StringBuilder("The following properties were null: ")
        } else {
            append(", ")
        }
            .append(propertyName)
            .apply {
                if (jsonName != propertyName) {
                    append(" (JSON name ").append(jsonName).append(')')
                }
            }

    @JvmStatic
    @JvmOverloads
    fun <T : Annotation> Class<T>.createJsonQualifierImplementation(annotationArguments: Map<String, Any> = emptyMap()): T {
        require(isAnnotation) { "$this must be an annotation." }
        @Suppress("UNCHECKED_CAST")
        return Proxy.newProxyInstance(classLoader, arrayOf<Class<*>>(this)) { proxy, method, args ->
            when (method.name) {
                "annotationType" -> this
                "equals" -> isInstance(args[0])
                "hashCode" -> 0
                "toString" -> "@$name()"
                else -> annotationArguments[method.name] ?:method.invoke(proxy, *args)
            }
        } as T
    }

    @JvmStatic
    fun JsonReader.nextFloat(): Float {
        val value = nextDouble().toFloat()
        // Double check for infinity after float conversion; many doubles > Float.MAX
        if (!isLenient && value.isInfinite()) {
            throw JsonDataException("JSON forbids NaN and infinities: $value at path $path")
        }
        return value
    }

    @JvmStatic
    fun JsonReader.nextByte(): Byte = nextIntInRange("a byte", -128, 255).toByte()

    @JvmStatic
    fun JsonReader.nextShort(): Short = nextIntInRange("a short", -32768, 32767).toShort()

    @JvmStatic
    fun JsonReader.nextChar(): Char {
        val value = nextString()
        if (value.length != 1) {
            throw JsonDataException(ERROR_FORMAT.format("a char", value, path))
        }
        return value[0]
    }

    @JvmStatic
    fun JsonWriter.byteValue(byte: Byte): JsonWriter = value(byte.toInt() and 0xff)

    @JvmStatic
    fun JsonWriter.byteValue(byte: Byte?): JsonWriter = if (byte == null) nullValue() else byteValue(byte)

    @JvmStatic
    fun JsonWriter.value(char: Char): JsonWriter = value(char.toString())

    @JvmStatic
    fun JsonWriter.value(char: Char?): JsonWriter = if (char == null) nullValue() else value(char)

    @JvmStatic
    private fun JsonReader.nextIntInRange(typeMessage: String, min: Int, max: Int): Int {
        val value = nextInt()
        if (value < min || value > max) {
            throw JsonDataException(ERROR_FORMAT.format(typeMessage, value, path))
        }
        return value
    }
}