@file:Suppress("NOTHING_TO_INLINE")

package se.ansman.kotshi

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.BYTE
import com.squareup.kotlinpoet.CHAR
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.tag
import com.squareup.moshi.Json
import com.squareup.moshi.JsonQualifier
import java.time.Instant
import javax.lang.model.SourceVersion
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.util.Elements

val STRING: ClassName = ClassName("kotlin", "String")
val JSON: ClassName = Json::class.java.asClassName()
val JSON_QUALIFIER = JsonQualifier::class.java

fun List<AnnotationSpec>?.jsonName(): String? =
    this?.find { it.className == JSON }?.let { spec ->
        val mirror = requireNotNull(spec.tag<AnnotationMirror>())
        mirror.elementValues.entries.single { it.key.simpleName.contentEquals("name") }.value.value as String
    }

fun List<AnnotationSpec>?.qualifiers(elements: Elements): Set<AnnotationSpec> {
    if (this == null) return setOf()
    return filterTo(mutableSetOf()) {
        elements.getTypeElement(it.className.toString()).getAnnotation(JSON_QUALIFIER) != null
    }
}

val TypeName.isPrimitive: Boolean
    get() = when (this) {
        BOOLEAN,
        BYTE,
        SHORT,
        INT,
        LONG,
        CHAR,
        FLOAT,
        DOUBLE -> true
        else -> false
    }

fun TypeName.nullable(): TypeName = if (isNullable) this else copy(nullable = true)
fun TypeName.notNull(): TypeName = if (isNullable) copy(nullable = false) else this

fun TypeSpec.Builder.maybeAddGeneratedAnnotation(elements: Elements, sourceVersion: SourceVersion) =
    apply {
        val generatedName = if (sourceVersion > SourceVersion.RELEASE_8) {
            "javax.annotation.processing.Generated"
        } else {
            "javax.annotation.Generated"
        }
        val typeElement = elements.getTypeElement(generatedName)
        if (typeElement != null) {
            addAnnotation(AnnotationSpec.builder(typeElement.asClassName())
                .addMember("%S", KotshiProcessor::class.java.canonicalName)
                .addMember("date = %S", Instant.now().toString())
                .addMember("comments = %S", "https://github.com/ansman/kotshi")
                .build())
        }
    }

inline fun FunSpec.Builder.addControlFlow(
    controlFlow: String,
    vararg args: Any,
    close: Boolean = true,
    block: FunSpec.Builder.() -> Unit
): FunSpec.Builder {
    beginControlFlow(controlFlow, *args)
    block()
    if (close) endControlFlow()
    return this
}

inline fun FunSpec.Builder.addNextControlFlow(
    controlFlow: String,
    vararg args: Any,
    close: Boolean = true,
    block: FunSpec.Builder.() -> Unit
): FunSpec.Builder {
    nextControlFlow(controlFlow, *args)
    block()
    if (close) endControlFlow()
    return this
}

/**
 * Adds a switch branch.
 *
 * A trailing -> is automatically inserted
 */
inline fun FunSpec.Builder.addWhenBranch(
    branch: String,
    vararg args: Any,
    block: FunSpec.Builder.() -> Unit
): FunSpec.Builder {
    beginControlFlow("$branch ->", *args)
    block()
    return endControlFlow()
}

/**
 * Adds a switch default.
 *
 * A trailing -> is automatically inserted
 */
inline fun FunSpec.Builder.addWhenElse(block: FunSpec.Builder.() -> Unit): FunSpec.Builder {
    beginControlFlow("else ->")
    block()
    return endControlFlow()
}

inline fun FunSpec.Builder.addIf(
    predicate: String,
    vararg args: Any,
    block: FunSpec.Builder.() -> Unit
): FunSpec.Builder =
    addControlFlow("if ($predicate)", *args) { block() }

inline fun FunSpec.Builder.addIfElse(
    predicate: String,
    vararg args: Any,
    block: FunSpec.Builder.() -> Unit
): FunSpec.Builder =
    addControlFlow("if ($predicate)", *args, close = false) { block() }

inline fun FunSpec.Builder.addElseIf(
    predicate: String,
    vararg args: Any,
    block: FunSpec.Builder.() -> Unit
): FunSpec.Builder =
    addNextControlFlow("else if ($predicate)", *args, close = false, block = block)

inline fun FunSpec.Builder.addElse(block: FunSpec.Builder.() -> Unit): FunSpec.Builder =
    addNextControlFlow("else") { block() }

inline fun FunSpec.Builder.addWhile(
    predicate: String,
    vararg args: Any,
    block: FunSpec.Builder.() -> Unit
): FunSpec.Builder =
    addControlFlow("while ($predicate)", *args, block = block)

inline fun FunSpec.Builder.addWhen(
    subject: String,
    vararg args: Any,
    block: FunSpec.Builder.() -> Unit
): FunSpec.Builder =
    addControlFlow("when ($subject)", *args, block = block)