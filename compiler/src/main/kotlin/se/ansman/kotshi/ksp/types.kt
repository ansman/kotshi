package se.ansman.kotshi.ksp

import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.isLocal
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.TypeParameterResolver
import com.squareup.kotlinpoet.ksp.toTypeParameterResolver
import com.squareup.kotlinpoet.ksp.toTypeVariableName

fun KSClassDeclaration.getAllConstructors(): Sequence<KSFunctionDeclaration> =
    (primaryConstructor?.let { sequenceOf(it) } ?: emptySequence()) + getConstructors()

fun KSClassDeclaration.superClass(): KSType? =
    superTypes.firstNotNullOfOrNull { superType ->
        superType.resolve().takeIf { (it.declaration as? KSClassDeclaration)?.classKind == ClassKind.CLASS }
    }

fun KSDeclaration.toTypeParameterResolver(): TypeParameterResolver {
    val typeParameters = (this as? KSClassDeclaration)?.typeParameters ?: emptyList()
    return typeParameters.toTypeParameterResolver(
        parentDeclaration?.toTypeParameterResolver(),
        qualifiedName?.asString() ?: "<unknown>"
    )
}

internal fun KSClassDeclaration.asTypeName(
    typeParameterResolver: TypeParameterResolver = toTypeParameterResolver(),
    actualTypeArgs: List<TypeName> = typeParameters.map { it.toTypeVariableName(typeParameterResolver) },
): TypeName {
    val className = asClassName()
    return if (typeParameters.isNotEmpty()) {
        className.parameterizedBy(actualTypeArgs)
    } else {
        className
    }
}

internal fun KSClassDeclaration.asClassName(): ClassName {
    require(!isLocal()) {
        "Local/anonymous classes are not supported!"
    }
    val pkgName = packageName.asString()
    val typesString = qualifiedName!!.asString().removePrefix("$pkgName.")

    val simpleNames = typesString
        .split(".")
    return ClassName(pkgName, simpleNames)
}