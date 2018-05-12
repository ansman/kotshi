@file:Suppress("NOTHING_TO_INLINE")

package se.ansman.kotshi

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.BYTE
import com.squareup.kotlinpoet.CHAR
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import me.eugeniomarletti.kotlin.metadata.KotlinClassMetadata
import me.eugeniomarletti.kotlin.metadata.shadow.metadata.ProtoBuf
import me.eugeniomarletti.kotlin.metadata.shadow.metadata.deserialization.NameResolver
import javax.lang.model.SourceVersion
import javax.lang.model.util.Elements
import kotlin.reflect.KClass

@JvmField
val STRING: ClassName = ClassName("kotlin", "String")

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

fun ProtoBuf.Class.asTypeName(nameResolver: NameResolver): TypeName {
    val className = asClassName(nameResolver)
    if (typeParameterCount == 0) {
        return className
    }
    return className.parameterizedBy(*typeParameterList.map { it.asTypeName(nameResolver, ::getTypeParameter) }.toTypedArray())
}

fun ProtoBuf.Class.asClassName(nameResolver: NameResolver): ClassName {
    val name = nameResolver.getQualifiedClassName(fqName)
    val packageEnd = name.lastIndexOf('/')

    val packageName: String
    val simpleNamesString: String

    if (packageEnd == -1) {
        packageName = ""
        simpleNamesString = name
    } else {
        packageName = name.substring(0, packageEnd)
        simpleNamesString = name.substring(packageEnd + 1, name.length)
    }
    val simpleNames = simpleNamesString.split('.')
    return ClassName(packageName.replace('/', '.'), simpleNames.first(), *simpleNames.subList(1, simpleNames.size).toTypedArray())
}

fun KotlinClassMetadata.asClassName() = data.classProto.asClassName(data.nameResolver)

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
                .addMember("%S", "se.ansman.kotshi.KotshiProcessor")
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
 * A trailing : and a break is automatically inserted
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
 * A trailing : and a break is automatically inserted
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

fun FileSpec.Builder.   addImport(import: Import) = addImport(import.className, *import.simpleNames.toTypedArray())
fun FileSpec.Builder.addImports(imports: Iterable<Import>) = applyEach(imports) { addImport(it) }

data class Import(val className: ClassName, val simpleNames: List<String>) {
    constructor(className: KClass<*>, vararg simpleNames: String) : this(className.asClassName(), simpleNames.asList())
    constructor(className: Class<*>, vararg simpleNames: String) : this(className.asClassName(), simpleNames.asList())
    constructor(className: ClassName, vararg simpleNames: String) : this(className, simpleNames.asList())
}

inline fun KClass<*>.import(vararg simpleNames: String) = Import(this, *simpleNames)
inline fun Class<*>.import(vararg simpleNames: String) = Import(this, *simpleNames)
inline fun ClassName.import(vararg simpleNames: String) = Import(this, *simpleNames)