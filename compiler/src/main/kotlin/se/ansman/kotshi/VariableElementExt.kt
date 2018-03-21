package se.ansman.kotshi

import javax.lang.model.element.VariableElement

fun VariableElement.getGetterName(): String =
    if (simpleName.startsWith("is")) {
        simpleName.toString()
    } else {
        "get${simpleName.toString().capitalize()}"
    }
