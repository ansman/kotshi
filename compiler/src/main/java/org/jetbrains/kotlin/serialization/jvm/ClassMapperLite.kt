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

package org.jetbrains.kotlin.serialization.jvm

import org.jetbrains.kotlin.name.ClassId
import java.util.*

object ClassMapperLite {
    val map: Map<String, String>

    // TODO: this seems very brittle, reuse JavaToKotlinClassMap from kotlin-reflect.jar somehow
    init {
        val primitives = listOf(
                "Unit", "V",
                "Boolean", "Z",
                "Char", "C",
                "Byte", "B",
                "Short", "S",
                "Int", "I",
                "Float", "F",
                "Long", "J",
                "Double", "D",
                "BooleanArray", "[Z",
                "CharArray", "[C",
                "ByteArray", "[B",
                "ShortArray", "[S",
                "IntArray", "[I",
                "FloatArray", "[F",
                "LongArray", "[J",
                "DoubleArray", "[D"
        )

        map = (0..primitives.lastIndex).step(2).map {
            i -> "kotlin/${primitives[i]}" to primitives[i + 1]
        }.toMap(LinkedHashMap()).apply {
            fun add(kotlinSimpleName: String, javaInternalName: String) {
                put("kotlin/$kotlinSimpleName", "L$javaInternalName;")
            }

            add("Any", "java/lang/Object")
            add("Nothing", "java/lang/Void")
            add("Annotation", "java/lang/annotation/Annotation")

            for (klass in listOf("String", "CharSequence", "Throwable", "Cloneable", "Number", "Comparable", "Enum")) {
                add("$klass", "java/lang/$klass")
            }

            for (klass in listOf("Iterator", "Collection", "List", "Set", "Map", "ListIterator")) {
                add("collections/$klass", "java/util/$klass")
                add("collections/Mutable$klass", "java/util/$klass")
            }

            add("collections/Iterable", "java/lang/Iterable")
            add("collections/MutableIterable", "java/lang/Iterable")
            add("collections/Map\$Entry", "java/util/Map\$Entry")
            add("collections/MutableMap\$MutableEntry", "java/util/Map\$Entry")

            for (i in 0..22) {
                add("Function$i", "kotlin/jvm/functions/Function$i")
            }

            // TODO: built-in companion intrinsics
        }
    }

    fun mapClass(classId: ClassId): String {
        val internalName = classId.asString().replace('.', '$')
        return map[internalName] ?: "L$internalName;"
    }
}
