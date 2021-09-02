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

import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.Taggable
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.tag

internal interface OriginatingKSFiles {
    val files: List<KSFile>
}

internal data class OriginatingKSFilesImpl(override val files: List<KSFile>) : OriginatingKSFiles

internal interface MutableOriginatingKSFiles : OriginatingKSFiles {
    override val files: MutableList<KSFile>
}

internal data class MutableOriginatingKSFilesImpl(override val files: MutableList<KSFile> = mutableListOf()) :
    MutableOriginatingKSFiles

internal fun TypeSpec.originatingKSFiles(): List<KSFile> = getKSFilesTag()
internal fun FunSpec.originatingKSFiles(): List<KSFile> = getKSFilesTag()
internal fun PropertySpec.originatingKSFiles(): List<KSFile> = getKSFilesTag()
internal fun FileSpec.originatingKSFiles(): List<KSFile> =
    members
        .flatMap {
            when (it) {
                is TypeSpec -> it.originatingKSFiles()
                is PropertySpec -> it.originatingKSFiles()
                is FunSpec -> it.originatingKSFiles()
                else -> emptyList() // TypeAlias
            }
        }
        .distinct()

private fun Taggable.getKSFilesTag(): List<KSFile> = tag<OriginatingKSFiles>()?.files.orEmpty()

internal fun TypeSpec.Builder.addOriginatingKSFile(ksFile: KSFile): TypeSpec.Builder = apply {
    getOrCreateKSFilesTag().add(ksFile)
}

internal fun FunSpec.Builder.addOriginatingKSFile(ksFile: KSFile): FunSpec.Builder = apply {
    getOrCreateKSFilesTag().add(ksFile)
}

internal fun PropertySpec.Builder.addOriginatingKSFile(ksFile: KSFile): PropertySpec.Builder = apply {
    getOrCreateKSFilesTag().add(ksFile)
}

private fun Taggable.Builder<*>.getOrCreateKSFilesTag(): MutableList<KSFile> {
    val holder = tags.getOrPut(
        OriginatingKSFiles::class, ::MutableOriginatingKSFilesImpl
    ) as MutableOriginatingKSFiles
    return holder.files
}