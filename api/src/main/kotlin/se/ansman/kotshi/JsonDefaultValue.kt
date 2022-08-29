package se.ansman.kotshi

/**
 * Annotation to be placed on enum an enum value or sealed class subclass to indicate that it is the default value if
 * an unknown or missing entry is encountered.
 *
 * Only one value can be annotated.
 *
 * If no entry is annotated the adapter will throw an exception if an unknown value is encountered.
 *
 * Example:
 * ```
 * @JsonSerializable
 * enum class SomeEnum {
 *     @JsonProperty(name = "some-value")
 *     SOME_VALUE,
 *     @JsonProperty(name = "some-other-value")
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
