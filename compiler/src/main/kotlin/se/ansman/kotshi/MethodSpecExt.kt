package se.ansman.kotshi

import com.squareup.javapoet.MethodSpec

inline fun MethodSpec.Builder.addControlFlow(
    controlFlow: String,
    vararg args: Any,
    close: Boolean = true,
    block: MethodSpec.Builder.() -> Unit
): MethodSpec.Builder {
    beginControlFlow(controlFlow, *args)
    block()
    if (close) endControlFlow()
    return this
}

inline fun MethodSpec.Builder.addNextControlFlow(
    controlFlow: String,
    vararg args: Any,
    close: Boolean = true,
    block: MethodSpec.Builder.() -> Unit
): MethodSpec.Builder {
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
inline fun MethodSpec.Builder.addSwitchBranch(
    branch: String,
    vararg args: Any,
    /** Terminating statement. Pass null for fall-through. */
    terminator: String? = "break",
    block: MethodSpec.Builder.() -> Unit
): MethodSpec.Builder {
    beginControlFlow("case $branch:", *args)
    block()
    terminator?.let { addStatement(it) }
    return endControlFlow()
}

/**
 * Adds a switch default.
 *
 * A trailing : and a break is automatically inserted
 */
inline fun MethodSpec.Builder.addSwitchDefault(
    /** Terminating statement. Pass null for fall-through. */
    terminator: String? = "break",
    block: MethodSpec.Builder.() -> Unit
): MethodSpec.Builder {
    beginControlFlow("default:")
    block()
    terminator?.let { addStatement(it) }
    return endControlFlow()
}

inline fun MethodSpec.Builder.addIf(
    predicate: String,
    vararg args: Any,
    block: MethodSpec.Builder.() -> Unit
): MethodSpec.Builder =
    addControlFlow("if ($predicate)", *args) { block() }

inline fun MethodSpec.Builder.addIfElse(
    predicate: String,
    vararg args: Any,
    block: MethodSpec.Builder.() -> Unit
): MethodSpec.Builder =
    addControlFlow("if ($predicate)", *args, close = false) { block() }

inline fun MethodSpec.Builder.addElseIf(
    predicate: String,
    vararg args: Any,
    block: MethodSpec.Builder.() -> Unit
): MethodSpec.Builder =
    addNextControlFlow("else if ($predicate)", *args, close = false) { block() }

inline fun MethodSpec.Builder.addElse(block: MethodSpec.Builder.() -> Unit): MethodSpec.Builder =
    addNextControlFlow("else") { block() }

inline fun MethodSpec.Builder.addWhile(
    predicate: String,
    vararg args: Any,
    block: MethodSpec.Builder.() -> Unit
): MethodSpec.Builder =
    addControlFlow("while ($predicate)", *args) { block() }

inline fun MethodSpec.Builder.addSwitch(
    predicate: String,
    vararg args: Any,
    block: MethodSpec.Builder.() -> Unit
): MethodSpec.Builder =
    addControlFlow("switch ($predicate)", *args) { block() }

