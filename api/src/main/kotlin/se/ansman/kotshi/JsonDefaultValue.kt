package se.ansman.kotshi

/**
 * Annotation to be placed on enum an enum value to indicate that specifies it as the default value
 * if the enum value is invalid.
 *
 * Only one value can be annotated.
 *
 * If no entry is annotated the adapter will throw an exception if an unknown constant is encountered.
 *
 * Example:
 * ```
 * @JsonSerializable
 * enum class SomeEnum {
 *     @Json(name = "some-value")
 *     SOME_VALUE,
 *     @Json(name = "some-other-value")
 *     SOME_OTHER_VALUE,
 *     @JsonDefaultValue
 *     UNKNOWN
 * }
 * ```
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.CLASS)
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
annotation class JsonDefaultValue
