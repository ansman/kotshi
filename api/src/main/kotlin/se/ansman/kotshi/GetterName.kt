package se.ansman.kotshi

/**
 * Annotation that must be applied when using [@JvmName][JvmName] to change the name of the getter for a property.
 *
 * The name given here must be the same as the one given to [JvmName].
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
annotation class GetterName(val value: String)