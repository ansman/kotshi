package se.ansman.kotshi

/**
 * Annotation to be placed on a sealed class to describe how it should be parsed.
 *
 * The label key should point out the key in the json to use when figuring out which subtype the json represents. The
 * value can be either a string or a number, in which case it's parsed as a string.
 *
 * Sub classes use the [PolymorphicLabel] annotation to indicate which value represents that subtype.
 *
 * If you want to use a fallback in case the type is invalid or missing you can annotate one of the sub types with
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
 * @param labelKey The key in the json that describes which subtype the object should be decoded as.
 */
annotation class Polymorphic(val labelKey: String)

/**
 * An annotation must be applied to subtypes of a sealed class to specify the value that represents the type.
 *
 * @see Polymorphic
 */
annotation class PolymorphicLabel(val value: String)