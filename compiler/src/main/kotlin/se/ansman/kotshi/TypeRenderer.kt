/*
 * Copyright (C) 2018 Square, Inc.
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

package se.ansman.kotshi

import com.squareup.kotlinpoet.*
import com.squareup.moshi.Types

/**
 * Renders literals like `Types.newParameterizedType(List::class.java, String::class.java)`.
 * Rendering is pluggable so that type variables can either be resolved or emitted as other code
 * blocks.
 */
abstract class TypeRenderer {
    abstract fun renderTypeVariable(typeVariable: TypeVariableName): CodeBlock

    fun render(typeName: TypeName, forceBox: Boolean = false): CodeBlock =
        renderInternal(typeName.unwrapTypeAlias(), forceBox)

    private fun renderInternal(typeName: TypeName, forceBox: Boolean): CodeBlock =
        when {
            typeName.annotations.isNotEmpty() -> render(typeName.copy(annotations = emptyList()), forceBox)
            typeName.isNullable -> renderObjectType(typeName.copy(nullable = false))
            else -> when (typeName) {
                is ClassName -> {
                    if (forceBox) {
                        renderObjectType(typeName)
                    } else {
                        CodeBlock.of("%T::class.java", typeName)
                    }
                }

                is ParameterizedTypeName -> {
                    // If it's an Array type, we shortcut this to return Types.arrayOf()
                    if (typeName.rawType == ARRAY) {
                        CodeBlock.of(
                            "%T.arrayOf(%L)",
                            Types::class,
                            renderObjectType(typeName.typeArguments[0])
                        )
                    } else {
                        val builder = CodeBlock.builder().apply {
                            add("%T.", Types::class)
                            val enclosingClassName = typeName.rawType.enclosingClassName()
                            if (enclosingClassName != null) {
                                add("newParameterizedTypeWithOwner(%L, ", render(enclosingClassName))
                            } else {
                                add("newParameterizedType(")
                            }
                            add("%T::class.java", typeName.rawType.unwrapTypeAlias())
                            for (typeArgument in typeName.typeArguments) {
                                add(", %L", renderObjectType(typeArgument))
                            }
                            add(")")
                        }
                        builder.build()
                    }
                }

                is WildcardTypeName -> {
                    val target: TypeName
                    val method: String
                    when {
                        typeName.inTypes.size == 1 -> {
                            target = typeName.inTypes[0]
                            method = "supertypeOf"
                        }
                        typeName.outTypes.size == 1 -> {
                            target = typeName.outTypes[0]
                            method = "subtypeOf"
                        }
                        else -> throw IllegalArgumentException(
                            "Unrepresentable wildcard type. Cannot have more than one bound: $typeName"
                        )
                    }
                    CodeBlock.of("%T.%L(%L)", Types::class, method, render(target, forceBox = true))
                }

                is TypeVariableName -> renderTypeVariable(typeName)

                else -> throw IllegalArgumentException("Unrepresentable type: $typeName")
            }
        }

    private fun renderObjectType(typeName: TypeName): CodeBlock =
        if (typeName.isPrimitive) {
            CodeBlock.of("%T::class.javaObjectType", typeName)
        } else {
            renderInternal(typeName, forceBox = false)
        }

    companion object {
        operator fun invoke(renderer: (TypeVariableName) -> CodeBlock): TypeRenderer =
            object : TypeRenderer() {
                override fun renderTypeVariable(typeVariable: TypeVariableName): CodeBlock = renderer(typeVariable)
            }
    }
}