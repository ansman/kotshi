package se.ansman.kotshi

@Retention(value = AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.TYPEALIAS, AnnotationTarget.PROPERTY)
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This is an internal Kotshi API that should not be used from outside of generated Kotshi code. No compatibility guarantees are provided."
)
annotation class InternalKotshiApi