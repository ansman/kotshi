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
import se.ansman.kotshi.kapt.KotshiProcessor

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

fun TypeSpec.Builder.addGeneratedAnnotation(annotationClass: ClassName, processorClass: ClassName) =
        addAnnotation(
            AnnotationSpec.builder(annotationClass)
                .addMember("%S", processorClass.canonicalName)
                .addMember("comments = %S", "https://github.com/ansman/kotshi")
                .build()
        )

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
    beginControlFlow("$branch·->", *args)
    block()
    return endControlFlow()
}

/**
 * Adds a switch default.
 *
 * A trailing -> is automatically inserted
 */
inline fun FunSpec.Builder.addWhenElse(block: FunSpec.Builder.() -> Unit): FunSpec.Builder {
    beginControlFlow("else·->")
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