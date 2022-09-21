package se.ansman.kotshi.renderer

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.ACC_PRIVATE
import org.objectweb.asm.Opcodes.ACC_PUBLIC
import org.objectweb.asm.Opcodes.ACC_STATIC
import org.objectweb.asm.Type
import org.objectweb.asm.commons.InstructionAdapter
import se.ansman.kotshi.asm.visitMethod
import se.ansman.kotshi.model.DefaultConstructorAccessor
import se.ansman.kotshi.model.delegateDescriptor
import se.ansman.kotshi.model.maskCount
import se.ansman.kotshi.model.targetTypeConstructorDescriptor

class DefaultConstructorAccessorsRenderer(private val accessors: List<DefaultConstructorAccessor>) {
    val className = "DefaultConstructorAccessors"
    fun render(targetPackage: String): ByteArray = with(ClassWriter(0)) {
        visit(
            /* version = */ Opcodes.V1_6,
            /* access = */ ACC_PUBLIC,
            /* name = */ "${targetPackage.replace('.', '/')}/$className",
            /* signature = */ null,
            /* superName = */ "java/lang/Object",
            /* interfaces = */ null
        )
        visitMethod(ACC_PRIVATE, "<init>", "()V") {}
        for (accessor in accessors) {
            val descriptor = accessor.delegateDescriptor()
            val type = Type.getMethodType(descriptor)
            visitMethod(
                ACC_PUBLIC + ACC_STATIC,
                accessor.accessorName,
                descriptor,
                // Check if needed for generic types
                null,
                null, // TODO?
            ) {
                val adapter = InstructionAdapter(this)
                visitAnnotation(NOT_NULL, false).visitEnd()
                accessor.parameters.forEachIndexed { index, parameter ->
                    visitParameter(parameter.name, Opcodes.ACC_FINAL)
                    if (parameter.type.length == 1) return@forEachIndexed
                    val annotation = when (parameter.nullability) {
                        DefaultConstructorAccessor.Parameter.Nullability.NOT_NULL -> NOT_NULL
                        DefaultConstructorAccessor.Parameter.Nullability.NULLABLE -> NULLABLE
                        DefaultConstructorAccessor.Parameter.Nullability.PLATFORM -> return@forEachIndexed
                    }
                    visitParameterAnnotation(index, annotation, false)
                }
                repeat(accessor.maskCount) { mask ->
                    visitParameter("mask${mask + 1}", Opcodes.ACC_FINAL)
                }
                visitCode()
                visitTypeInsn(Opcodes.NEW, accessor.targetType)
                visitInsn(Opcodes.DUP)
                var i = 0
                type.argumentTypes.forEach { t ->
                    adapter.load(i, t)
                    i += t.size
                }
                visitInsn(Opcodes.ACONST_NULL)
                visitMethodInsn(
                    Opcodes.INVOKESPECIAL,
                    accessor.targetType,
                    "<init>",
                    accessor.targetTypeConstructorDescriptor(),
                    false
                )
                visitInsn(Opcodes.ARETURN)

                /*
              // access flags 0x1
              // signature (Lse/ansman/kotshi/ClassWithGenericDefaults$Generic1<Ljava/lang/String;>;Lse/ansman/kotshi/ClassWithGenericDefaults$Generic2<Ljava/lang/String;Ljava/lang/Integer;>;Ljava/util/List<+Ljava/util/List<Ljava/lang/String;>;>;)V
              // declaration: void <init>(se.ansman.kotshi.ClassWithGenericDefaults$Generic1<java.lang.String>, se.ansman.kotshi.ClassWithGenericDefaults$Generic2<java.lang.String, java.lang.Integer>, java.util.List<? extends java.util.List<java.lang.String>>)
              public <init>(Lse/ansman/kotshi/ClassWithGenericDefaults$Generic1;Lse/ansman/kotshi/ClassWithGenericDefaults$Generic2;Ljava/util/List;)V
                // annotable parameter count: 3 (visible)
                // annotable parameter count: 3 (invisible)
                @Lorg/jetbrains/annotations/NotNull;() // invisible, parameter 0
                @Lorg/jetbrains/annotations/NotNull;() // invisible, parameter 1
                @Lorg/jetbrains/annotations/NotNull;() // invisible, parameter 2
               L0
                ALOAD 1
                LDC "generic1"
                INVOKESTATIC kotlin/jvm/internal/Intrinsics.checkNotNullParameter (Ljava/lang/Object;Ljava/lang/String;)V
                ALOAD 2
                LDC "generic2"
                INVOKESTATIC kotlin/jvm/internal/Intrinsics.checkNotNullParameter (Ljava/lang/Object;Ljava/lang/String;)V
                ALOAD 3
                LDC "list"
                INVOKESTATIC kotlin/jvm/internal/Intrinsics.checkNotNullParameter (Ljava/lang/Object;Ljava/lang/String;)V
               L1
                LINENUMBER 4 L1
                ALOAD 0
                INVOKESPECIAL java/lang/Object.<init> ()V
                ALOAD 0
                ALOAD 1
                PUTFIELD se/ansman/kotshi/ClassWithGenericDefaults.generic1 : Lse/ansman/kotshi/ClassWithGenericDefaults$Generic1;
                ALOAD 0
                ALOAD 2
                PUTFIELD se/ansman/kotshi/ClassWithGenericDefaults.generic2 : Lse/ansman/kotshi/ClassWithGenericDefaults$Generic2;
                ALOAD 0
                ALOAD 3
                PUTFIELD se/ansman/kotshi/ClassWithGenericDefaults.list : Ljava/util/List;
                RETURN
               L2
                LOCALVARIABLE this Lse/ansman/kotshi/ClassWithGenericDefaults; L0 L2 0
                LOCALVARIABLE generic1 Lse/ansman/kotshi/ClassWithGenericDefaults$Generic1; L0 L2 1
                LOCALVARIABLE generic2 Lse/ansman/kotshi/ClassWithGenericDefaults$Generic2; L0 L2 2
                LOCALVARIABLE list Ljava/util/List; L0 L2 3
                MAXSTACK = 2
                MAXLOCALS = 4
                 */
            }
        }
        visitEnd()
        toByteArray()
    }
}

private const val NOT_NULL = "Lorg/jetbrains/annotations/NotNull;"
private const val NULLABLE = "Lorg/jetbrains/annotations/Nullable;"