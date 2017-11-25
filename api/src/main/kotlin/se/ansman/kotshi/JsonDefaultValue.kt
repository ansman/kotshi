package se.ansman.kotshi

/**
 * An annotation that indicates that the target wants or provides a default value.
 *
 * When applied to a constructor property in a data class it indicates that if the value is null or absent in the JSON
 * a default value will be used instead.
 *
 * When applied to a function, property, constructor or enum type indicates that it's a provider of default values.
 * The provider must not return `null` unless annotated by `@Nullable`.
 *
 * You can also apply this annotation to another annotation for when you need multiple default values for different
 * properties.
 *
 * Example:
 * ```
 * @Target(AnnotationTarget.VALUE_PARAMETER,
 *         AnnotationTarget.FUNCTION,
 *         AnnotationTarget.CONSTRUCTOR,
 *         AnnotationTarget.FIELD,
 *         AnnotationTarget.PROPERTY_GETTER)
 * @MustBeDocumented
 * @Retention(AnnotationRetention.SOURCE)
 * annotation class StringWithNA
 *
 * @JsonSerializable
 * data class MyClass(
 *   @JsonDefaultValue
 *   val name: String,
 *   @StringWithNA
 *   val address: String
 * ) {
 *   companion object {
 *     @JsonDefaultValue
 *     @JvmField
 *     val defaultString = ""
 *
 *     @StringWithNA
 *     fun defaultStringWithNA() = "N/A"
 *   }
 * }
 * ```
 */
@Target(AnnotationTarget.VALUE_PARAMETER,
        AnnotationTarget.ANNOTATION_CLASS,
        AnnotationTarget.FUNCTION,
        AnnotationTarget.CONSTRUCTOR,
        AnnotationTarget.FIELD,
        AnnotationTarget.PROPERTY_GETTER)
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
annotation class JsonDefaultValue