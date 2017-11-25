package se.ansman.kotshi

/**
 * Tells Kotshi that you want this, non nullable, property to have a default value if the value is `null` or absent
 * in the Json.
 *
 * Simply annotate a property with `@JsonDefaultValue` and annotate a function or field with
 * [`@JsonDefaultValue`][JsonDefaultValue]:
 * ```
 * data class MyClass(@JsonDefaultValue val myProperty: String)
 *
 * @JsonDefaultValueProvider
 * fun provideDefaultJsonString() = "Some default value"
 * ```
 *
 * You can also annotate another annotation with this annotation to create a qualifier to allow providing different
 * values for the same type:
 * ```
 * @Target(AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
 * @MustBeDocumented
 * @Retention(AnnotationRetention.SOURCE)
 * @JsonDefaultValue
 * annotation class UseOtherDefaultValue
 *
 * data class MyClass(@UseOtherDefaultValue val myProperty: String)
 *
 * @UseOtherDefaultValue
 * fun provideOtherDefaultJsonString() = "Some other default value"
 * ```
 *
 * @see JsonDefaultValueProvider
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