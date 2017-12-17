package se.ansman.kotshi

inline fun <T> T.applyIf(applyIf: Boolean, block: T.() -> Unit): T = apply {
    if (applyIf) block(this)
}

inline fun <T, I> T.applyEach(iterable: Iterable<I>, block: T.(I) -> Unit): T = apply {
    iterable.forEach { block(this, it) }
}