package se.ansman.kotshi

/**
 * Annotations that must be used if there are multiple constructors to denote which one is the one that should be
 * treated as the primary constructor.
 *
 * All the arguments to the constructor must have a corresponding field and getter (unless the property is annotated
 * with [@JvmField][JvmField].
 *
 * Example:
 * ```
 * @JsonSerializable
 * data class Name @KotshiConstructor constructor(val firstName: String, val lastName: String) {
 *     constructor(fullName: String) : this(fullName.substringBefore(" "), fullName.substringAfter(" "))
 * }
 * ```
 */
@Target(AnnotationTarget.CONSTRUCTOR)
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
annotation class KotshiConstructor