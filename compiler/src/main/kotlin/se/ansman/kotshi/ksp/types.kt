/*
 * Copyright (C) 2021 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.ansman.kotshi.ksp

import com.google.devtools.ksp.isLocal
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeAlias
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Variance.CONTRAVARIANT
import com.google.devtools.ksp.symbol.Variance.COVARIANT
import com.google.devtools.ksp.symbol.Variance.STAR
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeVariableName
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets.UTF_8
import com.squareup.kotlinpoet.STAR as KpStar

internal fun KSType.toTypeName(): TypeName {
    val type = when (declaration) {
        is KSClassDeclaration -> (declaration as KSClassDeclaration).toTypeName(
            arguments.map { it.type!!.resolve().toTypeName() }
        )
        is KSTypeParameter -> (declaration as KSTypeParameter).toTypeName()
        is KSTypeAlias -> (declaration as KSTypeAlias).type.resolve().toTypeName()
        else -> error("Unsupported type: $declaration")
    }

    // TODO: Remove?
//    return type.copy(nullable = makeNullable() == this)
    return type.copy(nullable = isMarkedNullable)
}

internal fun KSClassDeclaration.toTypeName(
    actualTypeArgs: List<TypeName> = typeParameters.map { it.toTypeName() },
): TypeName {
    val className = toClassName()
    return if (typeParameters.isNotEmpty()) {
        className.parameterizedBy(actualTypeArgs)
    } else {
        className
    }
}

internal fun KSClassDeclaration.toClassName(): ClassName {
    require(!isLocal()) {
        "Local/anonymous classes are not supported!"
    }
    val pkgName = packageName.asString()
    val typesString = qualifiedName!!.asString().removePrefix("$pkgName.")

    val simpleNames = typesString
        .split(".")
    return ClassName(pkgName, simpleNames)
}

internal fun KSTypeParameter.toTypeName(): TypeName {
    if (variance == STAR) return KpStar
    val typeVarName = name.getShortName()
    val typeVarBounds = bounds.map { it.toTypeName() }.toList()
    val typeVarVariance = when (variance) {
        COVARIANT -> KModifier.IN
        CONTRAVARIANT -> KModifier.OUT
        else -> null
    }
    return TypeVariableName(typeVarName, bounds = typeVarBounds, variance = typeVarVariance)
}

internal fun KSTypeReference.toTypeName(): TypeName {
    val type = resolve()
    return type.toTypeName()
}

internal fun FileSpec.writeTo(codeGenerator: CodeGenerator) {
    val dependencies = Dependencies(false, *originatingKSFiles().toTypedArray())
    val file = codeGenerator.createNewFile(dependencies, packageName, name)
    // Don't use writeTo(file) because that tries to handle directories under the hood
    OutputStreamWriter(file, UTF_8)
        .use(::writeTo)
}