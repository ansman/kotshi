package se.ansman.kotshi

import com.squareup.moshi.JsonDataException

/**
 * Annotation to be placed on a sealed class to describe how it should be parsed.
 *
 * The label key should point out the key in the json to use when figuring out which subtype the json represents. The
 * value can be either a string or a number, in which case it's parsed as a string.
 *
 * Subclasses use the [PolymorphicLabel] annotation to indicate which value represents that subtype.
 *
 * If you want to use a fallback in case the type is invalid or missing you can annotate one of the subtypes with
 * [JsonDefaultValue].
 *
 * Example:
 * ```
 * @JsonSerializable
 * @Polymorphic(labelKey = "handType")
 * sealed class HandOfCards
 *
 * @JsonSerializable
 * @PolymorphicLabel("blackjack")
 * data class BlackjackHand(val hiddenCard: Card, val visibleCards: List<Card>) : HandOfCards()
 *
 * @JsonSerializable
 * @PolymorphicLabel("holdem")
 * data class HoldemHand(val hiddenCards: List<Card>) : HandOfCards()
 * ```
 *
 * @param labelKey the key in the json that describes which subtype the object should be decoded as.
 * @param onMissing defined what happens if [labelKey] is missing from the JSON.
 * @param onInvalid defined what happens if [labelKey] is present but invalid (unknown).
 */
@Target(AnnotationTarget.CLASS)
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
annotation class Polymorphic(
    val labelKey: String,
    val onMissing: Fallback = Fallback.DEFAULT,
    val onInvalid: Fallback = Fallback.DEFAULT
) {
    /**
     * Specifies what happens if the polymorphic label is invalid or missing during parsing of a sealed class.
     */
    enum class Fallback {
        /**
         * The default behavior which is to use the class annotated with [JsonDefaultValue] if it exists, otherwise
         * it behaves like [FAIL].
         */
        DEFAULT,
        /** Throw a [JsonDataException]. */
        FAIL,
        /** Return `null` */
        NULL
    }
}

/**
 * An annotation must be applied to subtypes of a sealed class to specify the value that represents the type.
 *
 * @see Polymorphic
 */
@Target(AnnotationTarget.CLASS)
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
annotation class PolymorphicLabel(val value: String)