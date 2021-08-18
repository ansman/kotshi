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
@InternalKotshiApi
object KotshiUtils {
    @JvmStatic
    @InternalKotshiApi
    val Type.typeArgumentsOrFail: Array<Type>
        get() = (this as? ParameterizedType)?.actualTypeArguments
            ?: throw IllegalArgumentException("""
                    ${Types.getRawType(this).simpleName} is generic and requires you to specify its type
                    arguments. Don't request an adapter using Type::class.java or value.javaClass but rather using
                    Types.newParameterizedType.
                """.trimIndent())

    @JvmStatic
    @JvmOverloads
    @InternalKotshiApi
    fun StringBuilder?.appendNullableError(propertyName: String, jsonName: String = propertyName): StringBuilder =
        (if (this == null) StringBuilder("The following properties were null: ") else append(", "))
            .append(propertyName)
            .apply {
                if (jsonName != propertyName) {
                    append(" (JSON name ").append(jsonName).append(')')
                }
            }

    @JvmStatic
    @JvmOverloads
    @InternalKotshiApi
    fun <T : Annotation> Class<T>.createJsonQualifierImplementation(annotationArguments: Map<String, Any> = emptyMap()): T {
        require(isAnnotation) { "$this must be an annotation." }
        @Suppress("UNCHECKED_CAST")
        return Proxy.newProxyInstance(classLoader, arrayOf<Class<*>>(this)) { proxy, method, args ->
            when (method.name) {
                "annotationType" -> this
                "equals" -> isInstance(args[0])
                "hashCode" -> 0
                "toString" -> "@$name()"
                else -> annotationArguments[method.name] ?: method.invoke(proxy, *args)
            }
        } as T
    }

    @JvmStatic
    @InternalKotshiApi
    fun JsonReader.nextFloat(): Float =
        nextDouble().toFloat().also {
            // Double check for infinity after float conversion; many doubles > Float.MAX
            if (!isLenient && it.isInfinite()) {
                throw JsonDataException("JSON forbids NaN and infinities: $it at path $path")
            }
        }

    @JvmStatic
    @InternalKotshiApi
    fun JsonReader.nextByte(): Byte = nextIntInRange("a byte", -128, 255).toByte()

    @JvmStatic
    fun JsonReader.nextShort(): Short = nextIntInRange("a short", -32768, 32767).toShort()

    @JvmStatic
    @InternalKotshiApi
    fun JsonReader.nextChar(): Char =
        nextString()
            .also {
                if (it.length != 1) {
                    throw JsonDataException("Expected a char but was $it at path $path")
                }
            }
            .single()

    @JvmStatic
    @InternalKotshiApi
    fun JsonWriter.byteValue(byte: Byte): JsonWriter = value(byte.toInt() and 0xff)

    @JvmStatic
    @InternalKotshiApi
    fun JsonWriter.byteValue(byte: Byte?): JsonWriter = if (byte == null) nullValue() else byteValue(byte)

    @JvmStatic
    @InternalKotshiApi
    fun JsonWriter.value(char: Char): JsonWriter = value(char.toString())

    @JvmStatic
    @InternalKotshiApi
    fun JsonWriter.value(char: Char?): JsonWriter = if (char == null) nullValue() else value(char)

    @JvmStatic
    private fun JsonReader.nextIntInRange(typeMessage: String, min: Int, max: Int): Int =
        nextInt().also {
            if (it !in min..max) {
                throw JsonDataException("Expected $typeMessage but was $it at path $path")
            }
        }
}