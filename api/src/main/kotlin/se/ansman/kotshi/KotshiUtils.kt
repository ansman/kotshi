package se.ansman.kotshi

import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Types
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
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
            ?: throw IllegalArgumentException(
                """
                    ${Types.getRawType(this).simpleName} is generic and requires you to specify its type
                    arguments. Don't request an adapter using Type::class.java or value.javaClass but rather using
                    Types.newParameterizedType.
                """.trimIndent()
            )

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
        return Proxy.newProxyInstance(classLoader, arrayOf<Class<*>>(this), object : InvocationHandler {
            private val annotationMethods = declaredMethods
                .onEach { method ->
                    val value = annotationArguments[method.name]
                    if (value == null) {
                        require(method.defaultValue != null) {
                            "Annotation value for method ${method.name} is missing"
                        }
                    } else {
                        val expectedType = when (val returnType = method.returnType) {
                            Boolean::class.java -> Boolean::class.javaObjectType
                            Byte::class.java -> Byte::class.javaObjectType
                            Char::class.java -> Char::class.javaObjectType
                            Double::class.java -> Double::class.javaObjectType
                            Float::class.java -> Float::class.javaObjectType
                            Int::class.java -> Int::class.javaObjectType
                            Long::class.java -> Long::class.javaObjectType
                            Short::class.java -> Short::class.javaObjectType
                            Void::class.java -> Void::class.javaObjectType
                            else -> returnType
                        }
                        require(expectedType.isInstance(value)) {
                            "Expected value for method ${method.name} to be of type $expectedType but was ${value.javaClass}"
                        }
                    }
                }
                .toList()

            init {
                val extraArguments = annotationArguments.keys - annotationMethods.map { it.name }
                require(extraArguments.isEmpty()) {
                    "Found annotation values for unknown methods: $extraArguments"
                }
            }

            override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any =
                when (method.name) {
                    "annotationType" -> this
                    "equals" -> args!![0] === proxy || isInstance(args!![0]) && annotationMethods.all { m ->
                        m.invoke(args[0]) == (annotationArguments[m.name] ?: m.defaultValue)
                    }
                    "hashCode" -> annotationMethods.fold(0) { hashCode, m ->
                        hashCode * 31 + (annotationArguments[m.name] ?: m.defaultValue).hashCode()
                    }
                    "toString" -> "@$name(${annotationArguments.entries.joinToString()})"
                    else -> annotationArguments[method.name] ?: method.defaultValue
                }
        }) as T
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