package se.ansman.kotshi

import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.WildcardTypeName
import javax.lang.model.element.*
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Types

class DefaultValueProvider(
        types: Types,
        val element: Element
) {

    val typeMirror: TypeMirror = when (element.kind) {
        ElementKind.CONSTRUCTOR -> element.enclosingElement.asType()
        ElementKind.METHOD -> MoreTypes.asExecutable(element.asType()).returnType
        ElementKind.FIELD,
        ElementKind.ENUM_CONSTANT -> element.asType()
        else -> throw ProcessingError("The default value provider must be a constructor, method or field", element)
    }

    val type = typeMirror.asTypeName()

    val qualifiers by lazy { element.getDefaultValueQualifiers() }

    val accessor: CodeBlock = element.findAccessor(types)
            ?: throw ProcessingError("Could not find a way to access this provider", element)

    val canReturnNull by lazy {
        if (type.isPrimitive) {
            return@lazy false
        }
        when (element.kind) {
            ElementKind.CONSTRUCTOR -> false
            ElementKind.ENUM_CONSTANT -> false
            ElementKind.FIELD -> {
                val variable = element as VariableElement
                Modifier.FINAL !in element.modifiers || variable.constantValue == null
            }
            else -> true
        }
    }

    val isNullable by lazy { canReturnNull && element.hasAnnotation("Nullable") }

    init {
        when (element.kind) {
            ElementKind.CONSTRUCTOR,
            ElementKind.METHOD -> {
                if (MoreTypes.asExecutable(element.asType()).parameterTypes.isNotEmpty()) {
                    throw ProcessingError("Default value provider cannot have arguments", element)
                }
            }
            else -> {
            }
        }

        if (!element.isPublic) {
            throw ProcessingError("The default value provider must be public", element)
        }

        (type as? ParameterizedTypeName)?.typeArguments?.forEach {
            if (it is WildcardTypeName) {
                throw ProcessingError("Wildcard providers are currently not supported: ${it.javaClass}", element)
            }
        }
    }
}

private fun Element.findAccessor(types: Types): CodeBlock? =
        when (requireNotNull(kind)) {
            ElementKind.ENUM,
            ElementKind.CLASS,
            ElementKind.INTERFACE -> CodeBlock.builder()
                    .apply { enclosingElement.findAccessor(types)?.let { add(it).add(".") } }
                    .add("\$T", asType().asTypeName().rawType)
                    .build()
            ElementKind.ENUM_CONSTANT,
            ElementKind.FIELD -> {
                CodeBlock.builder()
                        .apply {
                            if (Modifier.STATIC in modifiers) {
                                add(enclosingElement.findAccessor(types))
                            } else {
                                add(MoreElements.asType(enclosingElement).findInstanceAccessor(types) ?:
                                        throw ProcessingError("Could not find a way to access this class. " +
                                                "Does it have a static getInstance() method or INSTANCE field?", enclosingElement))
                            }
                        }
                        .add(".\$L", simpleName)
                        .build()
            }
            ElementKind.METHOD -> CodeBlock.builder()
                    .apply {
                        if (Modifier.STATIC in modifiers) {
                            add(enclosingElement.findAccessor(types))
                        } else {
                            add(MoreElements.asType(enclosingElement).findInstanceAccessor(types) ?:
                                    throw ProcessingError("Could not find a way to access this class. " +
                                            "Does it have a static getInstance() method or INSTANCE field?", enclosingElement))
                        }
                    }
                    .add(".\$L()", simpleName)
                    .build()
            ElementKind.CONSTRUCTOR -> CodeBlock.builder()
                    .apply {
                        add("new ")
                        add(enclosingElement.findAccessor(types))
                        if ((enclosingElement as TypeElement).typeParameters.isNotEmpty()) {
                            add("<>")
                        }
                        add("()")
                    }
                    .build()
            else -> null
        }

private fun TypeElement.findInstanceAccessor(types: Types): CodeBlock? =
        enclosedElements
                .asSequence()
                .filter { it.isPublic && Modifier.STATIC in it.modifiers }
                .filter {
                    when (it.kind) {
                        ElementKind.FIELD -> it.simpleName.toString().equals("instance", ignoreCase = true) &&
                                Modifier.FINAL in it.modifiers &&
                                types.isSameType(it.asType(), asType())
                        ElementKind.METHOD -> it.simpleName.contentEquals("getInstance") &&
                                MoreElements.asExecutable(it).let {
                                    types.isSameType(asType(), it.returnType) && it.parameters.isEmpty()
                                }
                        else -> false
                    }
                }
                .map { it.findAccessor(types) }
                .firstOrNull()
                ?:
                if (simpleName.contentEquals("Companion")) {
                    enclosingElement.enclosedElements
                            .asSequence()
                            .filter { it.isPublic }
                            .filter { Modifier.STATIC in it.modifiers }
                            .filter { it.kind == ElementKind.FIELD }
                            .filter { types.isSameType(it.asType(), asType()) }
                            .filter { it.simpleName.contentEquals("Companion") }
                            .firstOrNull()
                            ?.findAccessor(types)
                } else {
                    null
                }