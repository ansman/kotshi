package se.ansman.kotshi

import com.squareup.moshi.JsonQualifier
import javax.lang.model.element.Element

fun Element.getJsonQualifiers(): List<Element> = getQualifiers<JsonQualifier>().toList()

inline fun <reified T : Annotation> Element.getQualifiers(): Sequence<Element> = annotationMirrors
    .asSequence()
    .map { it.annotationType.asElement() }
    .filter { it.getAnnotation(T::class.java) != null }