package se.ansman.kotshi

import com.squareup.moshi.JsonQualifier
import javax.lang.model.element.Element

inline fun <reified T : Annotation> Element.hasAnnotation() = getAnnotation(T::class.java) != null

fun Element.getDefaultValueQualifiers(): List<Element> = getQualifiers<JsonDefaultValue>()

fun Element.getJsonQualifiers(): List<Element> = getQualifiers<JsonQualifier>()

inline fun <reified T : Annotation> Element.getQualifiers(): List<Element> = annotationMirrors
        .asSequence()
        .map { it.annotationType.asElement() }
        .filter { it.getAnnotation(T::class.java) != null }
        .toList()