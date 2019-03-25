package se.ansman.kotshi

inline fun <T> T.applyIf(applyIf: Boolean, block: T.() -> Unit): T = apply {
    if (applyIf) block(this)
}

inline fun <T, I> T.applyEach(iterable: Iterable<I>, block: T.(I) -> Unit): T = apply {
    iterable.forEach { block(this, it) }
}

inline fun <T, I> T.applyEachIndexed(iterable: Iterable<I>, block: T.(Int, I) -> Unit): T = apply {
    iterable.forEachIndexed { index, element ->  block(this, index, element) }
}

inline fun <T> T.runIf(runIf: Boolean, block: T.() -> T): T = if (runIf) block(this) else this