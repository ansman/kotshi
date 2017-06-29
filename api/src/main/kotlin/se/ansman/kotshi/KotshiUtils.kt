package se.ansman.kotshi

import com.squareup.moshi.JsonQualifier
import java.lang.reflect.Proxy

/**
 * A helper class for Kotshi.
 *
 * These functions should not be considered public and are subject to change without notice.
 */
object KotshiUtils {
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
}