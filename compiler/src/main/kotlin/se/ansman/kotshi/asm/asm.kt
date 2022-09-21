package se.ansman.kotshi.asm

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor

inline fun ClassWriter.visitMethod(
    access: Int,
    name: String,
    descriptor: String,
    signature: String? = null,
    exceptions: Array<String>? = null,
    block: MethodVisitor.() -> Unit,
) {
    val method = visitMethod(access, name, descriptor, signature, exceptions)
    var isSuccessful = true
    try {
        method.block()
    } catch (e: Exception) {
        isSuccessful = false
        throw e
    } finally {
        if (isSuccessful) {
            method.visitEnd()
        }
    }
}