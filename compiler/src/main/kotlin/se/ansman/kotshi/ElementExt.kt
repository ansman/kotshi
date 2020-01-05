package se.ansman.kotshi

import javax.lang.model.element.Element

val Element.metadata: Metadata
    get() = getAnnotation(Metadata::class.java)
        ?: throw ProcessingError("Class must be written in Kotlin", this)