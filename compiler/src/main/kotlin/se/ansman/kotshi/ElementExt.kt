package se.ansman.kotshi

import com.squareup.moshi.JsonQualifier
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.Modifier

inline fun <reified T : Annotation> Element.hasAnnotation() = getAnnotation(T::class.java) != null

fun Element.hasAnnotation(simpleName: String) =
    annotationMirrors.any { it.annotationType.asElement().simpleName.contentEquals(simpleName) }

fun Element.getDefaultValueQualifier(): Element? = getQualifiers<JsonDefaultValue>().firstOrNull()

fun Element.getJsonQualifiers(): List<Element> = getQualifiers<JsonQualifier>().toList()

inline fun <reified T : Annotation> Element.getQualifiers(): Sequence<Element> = annotationMirrors
    .asSequence()
    .map { it.annotationType.asElement() }
    .filter { it.getAnnotation(T::class.java) != null }

val Element.isPublic: Boolean
    get() = when (requireNotNull(kind)) {
        ElementKind.ANNOTATION_TYPE,
        ElementKind.PACKAGE,
        ElementKind.INTERFACE -> true
        ElementKind.CLASS,
        ElementKind.ENUM,
        ElementKind.ENUM_CONSTANT,
        ElementKind.FIELD,
        ElementKind.PARAMETER,
        ElementKind.METHOD,
        ElementKind.CONSTRUCTOR -> Modifier.PUBLIC in modifiers && enclosingElement.isPublic
        ElementKind.LOCAL_VARIABLE,
        ElementKind.EXCEPTION_PARAMETER,
        ElementKind.STATIC_INIT,
        ElementKind.INSTANCE_INIT,
        ElementKind.TYPE_PARAMETER,
        ElementKind.OTHER,
        ElementKind.RESOURCE_VARIABLE -> throw IllegalArgumentException("isPublic is not applicable")
    }