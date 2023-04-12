package se.ansman.kotshi.assertions

import assertk.Assert
import assertk.assertions.support.expected

inline fun <reified T : Any> Assert<Class<*>>.isAssignableTo() {
    isAssignableTo(T::class.java)
}

fun Assert<Class<*>>.isAssignableTo(other: Class<*>) {
    given {
        if (!other.isAssignableFrom(it)) {
            expected("$it to be assignable to $other")
        }
    }
}