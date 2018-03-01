package se.ansman.kotshi

import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonQualifier
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import java.lang.reflect.Proxy

/**
 * A helper class for Kotshi.
 *
 * These functions should not be considered public and are subject to change without notice.
 */
object KotshiUtils {
    private const val ERROR_FORMAT = "Expected %s but was %s at path %s"

    @JvmStatic
    fun appendNullableError(stringBuilder: StringBuilder?, propertyName: String): StringBuilder =
        if (stringBuilder == null) {
            StringBuilder("The following properties were null: ")
        } else {
            stringBuilder.append(", ")
        }.append(propertyName)

    @JvmStatic
    fun <T : Annotation> createJsonQualifierImplementation(annotationType: Class<T>): T {
        if (!annotationType.isAnnotation) {
            throw IllegalArgumentException("$annotationType must be an annotation.")
        }
        if (!annotationType.isAnnotationPresent(JsonQualifier::class.java)) {
            throw IllegalArgumentException("$annotationType must have @JsonQualifier.")
        }
        if (annotationType.declaredMethods.isNotEmpty()) {
            throw IllegalArgumentException("$annotationType must not declare methods.")
        }
        @Suppress("UNCHECKED_CAST")
        return Proxy.newProxyInstance(annotationType.classLoader, arrayOf<Class<*>>(annotationType)) { proxy, method, args ->
            when (method.name) {
                "annotationType" -> annotationType
                "equals" -> annotationType.isInstance(args[0])
                "hashCode" -> 0
                "toString" -> "@${annotationType.name}()"
                else -> method.invoke(proxy, *args)
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