package se.ansman.kotshi.kapt

import com.google.auto.common.MoreTypes
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.BYTE
import com.squareup.kotlinpoet.CHAR
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.DelicateKotlinPoetApi
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.asTypeName
import se.ansman.kotshi.model.AnnotationModel
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.AnnotationValueVisitor
import javax.lang.model.element.VariableElement
import javax.lang.model.type.ArrayType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

@OptIn(DelicateKotlinPoetApi::class)
class AnnotationModelValueVisitor : AnnotationValueVisitor<AnnotationModel.Value<*>, TypeMirror> {
    override fun visit(av: AnnotationValue, p: TypeMirror): AnnotationModel.Value<*> =
        throw UnsupportedOperationException()

    override fun visit(av: AnnotationValue?): AnnotationModel.Value<*> {
        throw UnsupportedOperationException()
    }

    override fun visitUnknown(av: AnnotationValue, p: TypeMirror): AnnotationModel.Value<*> {
        throw UnsupportedOperationException()
    }

    override fun visitType(t: TypeMirror, p: TypeMirror): AnnotationModel.Value<*> =
        AnnotationModel.Value.Class(t.asTypeName().toKotlinVersion() as ClassName)

    override fun visitBoolean(b: Boolean, p: TypeMirror): AnnotationModel.Value<*> = AnnotationModel.Value.Boolean(b)
    override fun visitByte(b: Byte, p: TypeMirror): AnnotationModel.Value<*> = AnnotationModel.Value.Byte(b)
    override fun visitChar(c: Char, p: TypeMirror): AnnotationModel.Value<*> = AnnotationModel.Value.Char(c)
    override fun visitDouble(d: Double, p: TypeMirror): AnnotationModel.Value<*> = AnnotationModel.Value.Double(d)
    override fun visitFloat(f: Float, p: TypeMirror): AnnotationModel.Value<*> = AnnotationModel.Value.Float(f)
    override fun visitInt(i: Int, p: TypeMirror): AnnotationModel.Value<*> = AnnotationModel.Value.Int(i)
    override fun visitLong(i: Long, p: TypeMirror): AnnotationModel.Value<*> = AnnotationModel.Value.Long(i)
    override fun visitShort(s: Short, p: TypeMirror): AnnotationModel.Value<*> = AnnotationModel.Value.Short(s)

    override fun visitString(s: String, p: TypeMirror): AnnotationModel.Value<*> = AnnotationModel.Value.String(s)

    override fun visitEnumConstant(c: VariableElement, p: TypeMirror): AnnotationModel.Value<*> =
        AnnotationModel.Value.Enum(p.asTypeName() as ClassName, c.simpleName.toString())

    override fun visitAnnotation(a: AnnotationMirror, p: TypeMirror): AnnotationModel.Value<*> =
        AnnotationModel.Value.Annotation(a.toAnnotationModel())

    override fun visitArray(vals: List<AnnotationValue>, p: TypeMirror): AnnotationModel.Value<*> {
        require(p is ArrayType)
        return when (p.kind) {
            TypeKind.BOOLEAN ->
                AnnotationModel.Value.Array.Boolean(vals.map { AnnotationModel.Value.Boolean(it.value as Boolean) })
            TypeKind.BYTE ->
                AnnotationModel.Value.Array.Byte(vals.map { AnnotationModel.Value.Byte(it.value as Byte) })
            TypeKind.SHORT ->
                AnnotationModel.Value.Array.Short(vals.map { AnnotationModel.Value.Short(it.value as Short) })
            TypeKind.INT ->
                AnnotationModel.Value.Array.Int(vals.map { AnnotationModel.Value.Int(it.value as Int) })
            TypeKind.LONG ->
                AnnotationModel.Value.Array.Long(vals.map { AnnotationModel.Value.Long(it.value as Long) })
            TypeKind.CHAR ->
                AnnotationModel.Value.Array.Char(vals.map { AnnotationModel.Value.Char(it.value as Char) })
            TypeKind.FLOAT ->
                AnnotationModel.Value.Array.Float(vals.map { AnnotationModel.Value.Float(it.value as Float) })
            TypeKind.DOUBLE ->
                AnnotationModel.Value.Array.Double(vals.map { AnnotationModel.Value.Double(it.value as Double) })
            else -> {
                when (val componentType = p.componentType.asTypeName()) {
                    BOOLEAN ->
                        AnnotationModel.Value.Array.Boolean(vals.map { AnnotationModel.Value.Boolean(it.value as Boolean) })
                    BYTE ->
                        AnnotationModel.Value.Array.Byte(vals.map { AnnotationModel.Value.Byte(it.value as Byte) })
                    SHORT ->
                        AnnotationModel.Value.Array.Short(vals.map { AnnotationModel.Value.Short(it.value as Short) })
                    INT ->
                        AnnotationModel.Value.Array.Int(vals.map { AnnotationModel.Value.Int(it.value as Int) })
                    LONG ->
                        AnnotationModel.Value.Array.Long(vals.map { AnnotationModel.Value.Long(it.value as Long) })
                    CHAR ->
                        AnnotationModel.Value.Array.Char(vals.map { AnnotationModel.Value.Char(it.value as Char) })
                    FLOAT ->
                        AnnotationModel.Value.Array.Float(vals.map { AnnotationModel.Value.Float(it.value as Float) })
                    DOUBLE ->
                        AnnotationModel.Value.Array.Double(vals.map { AnnotationModel.Value.Double(it.value as Double) })
                    else -> AnnotationModel.Value.Array.Object(
                        elementType = componentType.toKotlinVersion(),
                        value = vals.map { value ->
                            value.accept(
                                AnnotationModelValueVisitor(),
                                p.componentType
                            ) as AnnotationModel.Value.Object<*>
                        }
                    )
                }
            }

        }
    }

    companion object {
        fun AnnotationMirror.toAnnotationModel(): AnnotationModel = AnnotationModel(
            annotationName = annotationType.asTypeName() as ClassName,
            hasMethods = MoreTypes.asTypeElement(annotationType).enclosedElements.isNotEmpty(),
            values = elementValues.entries.associateBy({ it.key.simpleName.toString() }) { (k, v) ->
                v.accept(AnnotationModelValueVisitor(), k.returnType)
            }
        )
    }
}