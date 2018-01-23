package se.ansman.kotshi

import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.AnnotationValueVisitor
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeMirror

object KotshiAnnotationValueVisitor : AnnotationValueVisitor<Any, Any?> {
    override fun visitFloat(f: Float, p: Any?): Any = f
    override fun visitByte(b: Byte, p: Any?): Any = b
    override fun visitShort(s: Short, p: Any?): Any = s
    override fun visitChar(c: Char, p: Any?): Any = c
    override fun visitUnknown(av: AnnotationValue, p: Any?): Any = av.accept(this, p)
    override fun visit(av: AnnotationValue, p: Any?): Any = av.accept(this, p)
    override fun visit(av: AnnotationValue): Any = av.accept(this, null)
    override fun visitArray(vals: List<AnnotationValue>, p: Any?): Any = vals.map { it.accept(this, p) }
    override fun visitBoolean(b: Boolean, p: Any?): Any = b
    override fun visitLong(i: Long, p: Any?): Any = i
    override fun visitType(t: TypeMirror, p: Any?): Any = t
    override fun visitString(s: String, p: Any?): Any = s
    override fun visitDouble(d: Double, p: Any?): Any = d
    override fun visitEnumConstant(c: VariableElement, p: Any?): Any = c
    override fun visitAnnotation(a: AnnotationMirror, p: Any?): Any = a
    override fun visitInt(i: Int, p: Any?): Any = i
}