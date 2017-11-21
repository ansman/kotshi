package se.ansman.kotshi

/**
 * Tells Kotshi that you want this, non nullable, property to have a default value if the value is `null` or absent
 * in the Json.
 *
 * Simply annotate a property with `@UseJsonDefaultValue` and annotate a function or field with
 * [`@UseJsonDefaultValue`][UseJsonDefaultValue]:
 * ```
 * data class MyClass(@UseJsonDefaultValue val myProperty: String)
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
 * @UseJsonDefaultValue
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
        AnnotationTarget.ANNOTATION_CLASS)
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
annotation class UseJsonDefaultValue