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

package kotlinx.reflect.lite.impl

import kotlinx.reflect.lite.CallableMetadata
import kotlinx.reflect.lite.ClassMetadata
import kotlinx.reflect.lite.ConstructorMetadata
import kotlinx.reflect.lite.FunctionMetadata
import kotlinx.reflect.lite.ParameterMetadata
import kotlinx.reflect.lite.TypeMetadata
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.NameResolver
import org.jetbrains.kotlin.serialization.deserialization.TypeTable
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBufUtil
import java.lang.ref.WeakReference
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.util.*
import java.lang.reflect.Array as ReflectArray

internal class ClassMetadataImpl(
        private val proto: ProtoBuf.Class,
        private val nameResolver: NameResolver
) : ClassMetadata {
    private val typeTable = TypeTable(proto.typeTable)

    override val functions: Map<String, ProtoBuf.Function> by lazySoft {
        proto.functionList.map { function ->
            JvmProtoBufUtil.getJvmMethodSignature(function, nameResolver, typeTable)
                    ?.let { it to function }
        }.filterNotNull().toMap()
    }

    override val constructors: Map<String, ProtoBuf.Constructor> by lazySoft {
        proto.constructorList.map { constructor ->
            JvmProtoBufUtil.getJvmConstructorSignature(constructor, nameResolver, typeTable)
                    ?.let { it to constructor }
        }.filterNotNull().toMap()
    }

    override fun getFunction(method: Method): FunctionMetadata? {
        return functions[signature(method.name, method.parameterTypes, method.returnType)]
                ?.let { FunctionMetadataImpl(it, nameResolver) }
    }

    override fun getConstructor(constructor: Constructor<*>): ConstructorMetadata? {
        return constructors[signature("<init>", constructor.parameterTypes, Void.TYPE)]
                ?.let { ConstructorMetadataImpl(it, nameResolver) }
    }

    private fun signature(name: String, parameterTypes: Array<Class<*>>, returnType: Class<*>): String {
        return buildString {
            append(name)
            parameterTypes.joinTo(this, separator = "", prefix = "(", postfix = ")", transform = Class<*>::desc)
            append(returnType.desc())
        }
    }
}

internal abstract class CallableMetadataImpl : CallableMetadata

internal class FunctionMetadataImpl(
        private val proto: ProtoBuf.Function,
        private val nameResolver: NameResolver
) : CallableMetadataImpl(), FunctionMetadata {
    override val parameters: List<ParameterMetadata>
        get() = proto.valueParameterList.map { ParameterMetadataImpl(it, nameResolver) }

    override val returnType: TypeMetadata
        get() = TypeMetadataImpl(proto.returnType, nameResolver)
}

internal class ConstructorMetadataImpl(
        private val proto: ProtoBuf.Constructor,
        private val nameResolver: NameResolver
) : CallableMetadataImpl(), ConstructorMetadata {
    override val parameters: List<ParameterMetadata>
        get() = proto.valueParameterList.map { ParameterMetadataImpl(it, nameResolver) }
}

internal class ParameterMetadataImpl(
        private val proto: ProtoBuf.ValueParameter,
        private val nameResolver: NameResolver
) : ParameterMetadata {
    override val name: String?
        get() = nameResolver.getString(proto.name)

    override val type: TypeMetadata
        get() = TypeMetadataImpl(proto.type, nameResolver)
}

internal class TypeMetadataImpl(
        private val proto: ProtoBuf.Type,
        private val nameResolver: NameResolver
) : TypeMetadata {
    override val isNullable: Boolean
        get() = proto.nullable
}


internal object ReflectionLiteImpl {
    private val metadataFqName = "kotlin.Metadata"

    private val methods = WeakHashMap<Class<*>, WeakReference<MethodCache>>()

    @Suppress("UNCHECKED_CAST")
    private class MethodCache(val klass: Class<*>) {
        val k = klass.getDeclaredMethod("k").let { method ->
            { instance: Annotation -> method(instance) as Int }
        }
        val d1 = klass.getDeclaredMethod("d1").let { method ->
            { instance: Annotation -> method(instance) as Array<String> }
        }
        val d2 = klass.getDeclaredMethod("d2").let { method ->
            { instance: Annotation -> method(instance) as Array<String> }
        }
    }

    fun loadClassMetadata(klass: Class<*>): ClassMetadata? {
        val annotation = klass.declaredAnnotations.singleOrNull { it.annotationClass.java.name == metadataFqName } ?: return null
        val metadataClass = annotation.annotationClass.java
        val methods = methods[metadataClass]?.get() ?: MethodCache(metadataClass).apply {
            methods[metadataClass] = WeakReference(this)
        }

        // Should be a class (kind = 1)
        if (methods.k(annotation) != 1) return null

        val (nameResolver, classProto) = JvmProtoBufUtil.readClassDataFrom(methods.d1(annotation), methods.d2(annotation))
        return ClassMetadataImpl(classProto, nameResolver)
    }
}
