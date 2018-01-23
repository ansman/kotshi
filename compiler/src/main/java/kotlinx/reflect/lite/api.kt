/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlinx.reflect.lite

import kotlinx.reflect.lite.impl.ReflectionLiteImpl
import org.jetbrains.kotlin.serialization.ProtoBuf
import java.lang.reflect.Constructor
import java.lang.reflect.Method

interface ClassMetadata {
    val functions: Map<String, ProtoBuf.Function>
    val constructors: Map<String, ProtoBuf.Constructor>

    fun getFunction(method: Method): FunctionMetadata?

    fun getConstructor(constructor: Constructor<*>): ConstructorMetadata?
}

interface CallableMetadata {
    val parameters: List<ParameterMetadata>
}

interface ConstructorMetadata : CallableMetadata

interface FunctionMetadata : CallableMetadata {
    val returnType: TypeMetadata
}

interface ParameterMetadata {
    val name: String?

    val type: TypeMetadata
}

interface TypeMetadata {
    val isNullable: Boolean
}



object ReflectionLite {
    fun loadClassMetadata(klass: Class<*>): ClassMetadata? {
        return ReflectionLiteImpl.loadClassMetadata(klass)
    }
}
