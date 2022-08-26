package se.ansman.kotshi.kapt

import javax.lang.model.element.AnnotationMirror

inline fun <reified R : Any> AnnotationMirror.getValue(name: String): R =
    getValueOrNull(name) ?: throw IllegalArgumentException("Annotation $this has no value $name")

inline fun <reified R : Any> AnnotationMirror.getValueOrNull(name: String): R? =
    elementValues.entries
        .find { it.key.simpleName.contentEquals(name) }
        ?.value
        ?.value as R?