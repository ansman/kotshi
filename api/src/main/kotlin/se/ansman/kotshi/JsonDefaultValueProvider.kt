package se.ansman.kotshi

/**
 * Annotation to indicate that a function, constructor or field can provide default values for properties annotated
 * with [`@UseJsonDefaultValue`][UseJsonDefaultValue].
 *
 * The target must have the following properties:
 * - Be public and be in a public class.
 * - Can accept no arguments.
 * - Cannot return null values.
 * - Cannot provide wildcard types.
 *
 * Example:
 * ```
 * data class MyClass(@UseJsonDefaultValue val myProperty: String)
 *
 * @JsonDefaultValueProvider
 * fun provideDefaultJsonString() = "Some default value"
 * ```
 *
 * If you want to provide different default values for the same type you can use a qualifier:
 * ```
 * @Target(AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
 * @MustBeDocumented
 * @Retention(AnnotationRetention.SOURCE)
 * @UseJsonDefaultValue
 * annotation class UseOtherDefaultValue
 *
 * data class MyClass(@UseOtherDefaultValue val myProperty: String)
 *
 * @UseOtherDefaultValue
 * fun provideOtherDefaultJsonString() = "Some other default value"
 * ```
 */
@Target(AnnotationTarget.FUNCTION,
        AnnotationTarget.CONSTRUCTOR,
        AnnotationTarget.FIELD,
        AnnotationTarget.PROPERTY_GETTER)
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
annotation class JsonDefaultValueProvider