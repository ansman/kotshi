@file:Suppress("unused")

package se.ansman.kotshi

import com.squareup.kotlinpoet.CodeBlock

inline fun CodeBlock.Builder.addControlFlow(
    controlFlow: String,
    vararg args: Any,
    close: Boolean = true,
    block: CodeBlock.Builder.() -> Unit
): CodeBlock.Builder {
    beginControlFlow(controlFlow, *args)
    block()
    if (close) endControlFlow()
    return this
}

inline fun CodeBlock.Builder.addNextControlFlow(
    controlFlow: String,
    vararg args: Any,
    close: Boolean = true,
    block: CodeBlock.Builder.() -> Unit
): CodeBlock.Builder {
    nextControlFlow(controlFlow, *args)
    block()
    if (close) endControlFlow()
    return this
}

/**
 * Adds a switch branch.
 *
 * A trailing : and a break is automatically inserted
 */
inline fun CodeBlock.Builder.addWhenBranch(
    branch: String,
    vararg args: Any,
    block: CodeBlock.Builder.() -> Unit
): CodeBlock.Builder {
    beginControlFlow("$branch:", *args)
    block()
    return endControlFlow()
}

inline fun CodeBlock.Builder.addIf(
    predicate: String,
    vararg args: Any,
    block: CodeBlock.Builder.() -> Unit
): CodeBlock.Builder = addControlFlow("if ($predicate)", *args) { block() }

inline fun CodeBlock.Builder.addIfElse(
    predicate: String,
    vararg args: Any,
    block: CodeBlock.Builder.() -> Unit
): CodeBlock.Builder = addControlFlow("if ($predicate)", *args, close = false) { block() }

inline fun CodeBlock.Builder.addElseIf(
    predicate: String,
    vararg args: Any,
    block: CodeBlock.Builder.() -> Unit
): CodeBlock.Builder = addNextControlFlow("else if ($predicate)", *args, close = false) { block() }

inline fun CodeBlock.Builder.addElse(block: CodeBlock.Builder.() -> Unit): CodeBlock.Builder =
    addNextControlFlow("else") { block() }

inline fun CodeBlock.Builder.addWhile(
    predicate: String,
    vararg args: Any,
    block: CodeBlock.Builder.() -> Unit
): CodeBlock.Builder = addControlFlow("while ($predicate)", *args) { block() }

inline fun CodeBlock.Builder.addWhen(
    predicate: String,
    vararg args: Any,
    block: CodeBlock.Builder.() -> Unit
): CodeBlock.Builder = addControlFlow("switch ($predicate)", *args) { block() }

