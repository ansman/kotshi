package se.ansman.kotshi.ksp

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueArgument
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import se.ansman.kotshi.JSON_QUALIFIER

inline fun <reified T : Annotation> KSAnnotated.getAnnotation(): KSAnnotation? = getAnnotation(T::class.java)

fun KSAnnotated.getAnnotation(type: Class<out Annotation>): KSAnnotation? =
    annotations.getAnnotation(type)

inline fun <reified T : Annotation> Sequence<KSAnnotation>.getAnnotation(): KSAnnotation? = getAnnotation(T::class.java)

fun Sequence<KSAnnotation>.getAnnotation(type: Class<out Annotation>): KSAnnotation? =
    firstOrNull {
        it.shortName.asString() == type.simpleName &&
            it.annotationType.resolve().declaration.qualifiedName?.asString() == type.name
    }

inline fun <reified T : Annotation> KSAnnotation.isAnnotation(): Boolean = isAnnotation(T::class.java)

fun KSAnnotation.isAnnotation(type: Class<out Annotation>): Boolean =
    shortName.asString() == type.simpleName &&
        annotationType.resolve().declaration.qualifiedName?.asString() == type.name

fun List<KSValueArgument>.asKeyValueMap(): Map<String, Any?> = associateBy({ it.name!!.asString() }, { it.value })

inline fun <reified V> KSAnnotation.getValue(name: String): V =
    arguments.first { it.name?.asString() == name }.value as V

inline fun <reified V : Enum<V>> KSAnnotation.getEnumValue(name: String, defaultValue: V): V =
    getValue<KSType?>(name)?.let { enumValueOf<V>(it.declaration.simpleName.getShortName()) } ?: defaultValue

fun KSAnnotation.isJsonQualifier(): Boolean =
    annotationType.resolve().declaration.annotations.any {
        it.shortName.asString() == JSON_QUALIFIER.simpleName &&
            it.annotationType.resolve().declaration.qualifiedName?.asString() == JSON_QUALIFIER.name
    }


internal fun KSAnnotation.toAnnotationSpec(resolver: Resolver): AnnotationSpec {
    val element = annotationType.resolve().declaration as KSClassDeclaration
    // TODO support generic annotations
    val builder = AnnotationSpec.builder(element.toClassName())
        .tag(KSAnnotation::class.java, this)
    for (argument in arguments) {
        val member = CodeBlock.builder()
        val name = argument.name!!.getShortName()
        member.add("%L = ", name)
        when (val value = argument.value!!) {
            resolver.builtIns.arrayType -> {
//        TODO("Arrays aren't supported tet")
//        member.add("[⇥⇥")
//        values.forEachIndexed { index, value ->
//          if (index > 0) member.add(", ")
//          value.accept(this, name)
//        }
//        member.add("⇤⇤]")
            }
            is KSType -> member.add("%T::class", value.toTypeName())
            // TODO is this the right way to handle an enum constant?
            is KSName ->
                member.add(
                    "%T.%L", ClassName.bestGuess(value.getQualifier()),
                    value.getShortName()
                )
            is KSAnnotation -> member.add("%L", value.toAnnotationSpec(resolver))
            else -> member.add(memberForValue(value))
        }
        builder.addMember(member.build())
    }
    return builder.build()
}

/**
 * Creates a [CodeBlock] with parameter `format` depending on the given `value` object.
 * Handles a number of special cases, such as appending "f" to `Float` values, and uses
 * `%L` for other types.
 */
private fun memberForValue(value: Any) = when (value) {
    is Class<*> -> CodeBlock.of("%T::class", value)
    is Enum<*> -> CodeBlock.of("%T.%L", value.javaClass, value.name)
    is String -> CodeBlock.of("%S", value)
    is Float -> CodeBlock.of("%Lf", value)
    is Char -> CodeBlock.of("'%L'", if (value == '\'') "\\'" else value)
    else -> CodeBlock.of("%L", value)
}