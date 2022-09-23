package se.ansman.kotshi

import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Types
import java.lang.reflect.*

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
    @InternalKotshiApi
    fun matches(
        targetType: Type?,
        actualType: Type?,
        allowSubClasses: Boolean = false,
        allowSuperClasses: Boolean = false,
    ): Boolean = when (targetType) {
        // e.g. Array<...>
        is GenericArrayType -> actualType is GenericArrayType &&
            matches(targetType.genericComponentType, actualType.genericComponentType, allowSubClasses = true)
        // e.g. List<...>
        is ParameterizedType -> actualType is ParameterizedType &&
            matches(actualType.rawType, targetType.rawType, allowSubClasses, allowSuperClasses) &&
            matches(actualType.ownerType, targetType.ownerType, allowSubClasses, allowSuperClasses) &&
            targetType.actualTypeArguments.size == actualType.actualTypeArguments.size &&
            targetType.actualTypeArguments.allIndexed { i, e -> matches(e, actualType.actualTypeArguments[i]) }
        // e.g. E : Number
        is TypeVariable<*> -> targetType.bounds.all { matches(it, actualType, allowSubClasses = true) }
        // e.g. out Number or in Number
        is WildcardType -> targetType.lowerBounds.all { matches(it, actualType, allowSuperClasses = true) } &&
            targetType.upperBounds.all { matches(it, actualType, allowSubClasses = true) }
        // e.g. String
        is Class<*> -> {
            fun Type.rawType(): Class<*>? =
                when (this) {
                    is GenericArrayType -> Array::class.java
                    is ParameterizedType -> rawType.rawType()
                    is TypeVariable<*> -> null // Type variables should not appear in actual type
                    is WildcardType -> (lowerBounds.singleOrNull() ?: upperBounds[0]).rawType()
                    is Class<*> -> this
                    else -> null
                }
            val rawType = actualType?.rawType()
            rawType != null && when {
                allowSubClasses -> targetType.isAssignableFrom(rawType)
                allowSuperClasses -> rawType.isAssignableFrom(targetType)
                else -> targetType == rawType
            }
        }
        null -> actualType == null
        // Unknown type
        else -> false
    }

    private inline fun <E> Array<E>.allIndexed(predicate: (index: Int, element: E) -> Boolean): Boolean {
        var i = 0
        while (i < size) {
            if (!predicate(i, get(i))) {
                return false
            }
            ++i
        }
        return true
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
                .apply { sortBy { it.name } }

            init {
                val extraArguments =
                    annotationArguments.keys - annotationMethods.mapTo(HashSet(annotationMethods.size)) { it.name }
                require(extraArguments.isEmpty()) {
                    "Found annotation values for unknown methods: $extraArguments"
                }
            }

            override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any =
                when (method.name) {
                    "annotationType" -> this@createJsonQualifierImplementation
                    "equals" -> args!![0] === proxy || isInstance(args!![0]) && annotationMethods.all { m ->
                        annotationValuesEquals(m.invoke(args[0]), annotationArguments[m.name] ?: m.defaultValue)
                    }
                    // For the implementation see https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/annotation/Annotation.html#hashCode()
                    "hashCode" -> annotationMethods.sumOf { m ->
                        (m.name.hashCode() * 127) xor (annotationArguments[m.name] ?: m.defaultValue).annotationValueHashCode()
                    }

                    "toString" -> buildString {
                        append('@').append(name)
                        append('(')
                        var appendSeparator = false
                        for (m in annotationMethods) {
                            if (appendSeparator) {
                                append(", ")
                            }
                            val value = annotationArguments[m.name] ?: m.defaultValue
                            append(m.name).append('=').append(value.annotationValueToString())
                            appendSeparator = true
                        }
                        append(')')
                        "@$name(${annotationArguments.entries.joinToString()})"
                    }

                    else -> annotationArguments[method.name] ?: method.defaultValue
                }

            private fun annotationValuesEquals(v1: Any?, v2: Any?): Boolean =
                when (v1) {
                    is BooleanArray -> v2 is BooleanArray && v1.contentEquals(v2)
                    is ByteArray -> v2 is ByteArray && v1.contentEquals(v2)
                    is CharArray -> v2 is CharArray && v1.contentEquals(v2)
                    is ShortArray -> v2 is ShortArray && v1.contentEquals(v2)
                    is IntArray -> v2 is IntArray && v1.contentEquals(v2)
                    is LongArray -> v2 is LongArray && v1.contentEquals(v2)
                    is FloatArray -> v2 is FloatArray && v1.contentEquals(v2)
                    is DoubleArray -> v2 is DoubleArray && v1.contentEquals(v2)
                    is Array<*> -> v2 is Array<*> && v1.contentEquals(v2)
                    else -> v1 == v2
                }

            private fun Any.annotationValueHashCode(): Int =
                when (this) {
                    is BooleanArray -> contentHashCode()
                    is ByteArray -> contentHashCode()
                    is CharArray -> contentHashCode()
                    is ShortArray -> contentHashCode()
                    is IntArray -> contentHashCode()
                    is LongArray -> contentHashCode()
                    is FloatArray -> contentHashCode()
                    is DoubleArray -> contentHashCode()
                    is Array<*> -> contentHashCode()
                    else -> hashCode()
                }

            private fun Any.annotationValueToString(): String =
                when (this) {
                    is BooleanArray -> contentToString()
                    is ByteArray -> contentToString()
                    is CharArray -> contentToString()
                    is ShortArray -> contentToString()
                    is IntArray -> contentToString()
                    is LongArray -> contentToString()
                    is FloatArray -> contentToString()
                    is DoubleArray -> contentToString()
                    is Array<*> -> contentToString()
                    else -> toString()
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