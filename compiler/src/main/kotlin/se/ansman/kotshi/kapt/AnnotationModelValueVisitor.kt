package se.ansman.kotshi.kapt

import com.google.auto.common.MoreTypes
import com.squareup.kotlinpoet.BOOLEAN_ARRAY
import com.squareup.kotlinpoet.BYTE_ARRAY
import com.squareup.kotlinpoet.CHAR_ARRAY
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.DOUBLE_ARRAY
import com.squareup.kotlinpoet.DelicateKotlinPoetApi
import com.squareup.kotlinpoet.FLOAT_ARRAY
import com.squareup.kotlinpoet.INT_ARRAY
import com.squareup.kotlinpoet.LONG_ARRAY
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.SHORT_ARRAY
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.U_BYTE
import com.squareup.kotlinpoet.U_BYTE_ARRAY
import com.squareup.kotlinpoet.U_INT
import com.squareup.kotlinpoet.U_INT_ARRAY
import com.squareup.kotlinpoet.U_LONG
import com.squareup.kotlinpoet.U_LONG_ARRAY
import com.squareup.kotlinpoet.U_SHORT
import com.squareup.kotlinpoet.U_SHORT_ARRAY
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.tags.TypeAliasTag
import kotlin.metadata.KmClassifier
import kotlin.metadata.KmFlexibleTypeUpperBound
import kotlin.metadata.KmType
import kotlin.metadata.KmTypeProjection
import kotlin.metadata.KmVariance
import kotlin.metadata.isNullable
import kotlin.metadata.isSecondary
import se.ansman.kotshi.model.AnnotationModel
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.AnnotationValueVisitor
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeMirror

@OptIn(DelicateKotlinPoetApi::class)
class AnnotationModelValueVisitor(
    private val metadataAccessor: MetadataAccessor
) : AnnotationValueVisitor<AnnotationModel.Value<*>, TypeName> {
    override fun visit(av: AnnotationValue, p: TypeName): AnnotationModel.Value<*> =
        throw UnsupportedOperationException()

    override fun visit(av: AnnotationValue?): AnnotationModel.Value<*> {
        throw UnsupportedOperationException()
    }

    override fun visitUnknown(av: AnnotationValue, p: TypeName): AnnotationModel.Value<*> {
        throw UnsupportedOperationException()
    }

    override fun visitType(t: TypeMirror, p: TypeName): AnnotationModel.Value<*> =
        AnnotationModel.Value.Class(t.asTypeName().toKotlinVersion() as ClassName)

    override fun visitBoolean(b: Boolean, p: TypeName): AnnotationModel.Value<*> = AnnotationModel.Value.Boolean(b)
    override fun visitByte(b: Byte, p: TypeName): AnnotationModel.Value<*> =
        if (p == U_BYTE) AnnotationModel.Value.UByte(b.toUByte()) else AnnotationModel.Value.Byte(b)
    override fun visitChar(c: Char, p: TypeName): AnnotationModel.Value<*> = AnnotationModel.Value.Char(c)
    override fun visitDouble(d: Double, p: TypeName): AnnotationModel.Value<*> = AnnotationModel.Value.Double(d)
    override fun visitFloat(f: Float, p: TypeName): AnnotationModel.Value<*> = AnnotationModel.Value.Float(f)
    override fun visitInt(i: Int, p: TypeName): AnnotationModel.Value<*> =
        if (p == U_INT) AnnotationModel.Value.UInt(i.toUInt()) else AnnotationModel.Value.Int(i)
    override fun visitLong(i: Long, p: TypeName): AnnotationModel.Value<*> =
        if (p == U_LONG) AnnotationModel.Value.ULong(i.toULong()) else AnnotationModel.Value.Long(i)
    override fun visitShort(s: Short, p: TypeName): AnnotationModel.Value<*> =
        if (p == U_SHORT) AnnotationModel.Value.UShort(s.toUShort()) else AnnotationModel.Value.Short(s)

    override fun visitString(s: String, p: TypeName): AnnotationModel.Value<*> = AnnotationModel.Value.String(s)

    override fun visitEnumConstant(c: VariableElement, p: TypeName): AnnotationModel.Value<*> =
        AnnotationModel.Value.Enum(p as ClassName, c.simpleName.toString())

    override fun visitAnnotation(a: AnnotationMirror, p: TypeName): AnnotationModel.Value<*> =
        AnnotationModel.Value.Annotation(a.toAnnotationModel(metadataAccessor))

    override fun visitArray(vals: List<AnnotationValue>, p: TypeName): AnnotationModel.Value<*> =
        when (p) {
            BOOLEAN_ARRAY -> AnnotationModel.Value.Array.Boolean(vals.map { AnnotationModel.Value.Boolean(it.value as Boolean) })
            BYTE_ARRAY -> AnnotationModel.Value.Array.Byte(vals.map { AnnotationModel.Value.Byte(it.value as Byte) })
            U_BYTE_ARRAY -> AnnotationModel.Value.Array.UByte(vals.map { AnnotationModel.Value.UByte((it.value as Byte).toUByte()) })
            SHORT_ARRAY -> AnnotationModel.Value.Array.Short(vals.map { AnnotationModel.Value.Short(it.value as Short) })
            U_SHORT_ARRAY -> AnnotationModel.Value.Array.UShort(vals.map { AnnotationModel.Value.UShort((it.value as Short).toUShort()) })
            INT_ARRAY -> AnnotationModel.Value.Array.Int(vals.map { AnnotationModel.Value.Int(it.value as Int) })
            U_INT_ARRAY -> AnnotationModel.Value.Array.UInt(vals.map { AnnotationModel.Value.UInt((it.value as Int).toUInt()) })
            LONG_ARRAY -> AnnotationModel.Value.Array.Long(vals.map { AnnotationModel.Value.Long(it.value as Long) })
            U_LONG_ARRAY -> AnnotationModel.Value.Array.ULong(vals.map { AnnotationModel.Value.ULong((it.value as Long).toULong()) })
            CHAR_ARRAY -> AnnotationModel.Value.Array.Char(vals.map { AnnotationModel.Value.Char(it.value as Char) })
            FLOAT_ARRAY -> AnnotationModel.Value.Array.Float(vals.map { AnnotationModel.Value.Float(it.value as Float) })
            DOUBLE_ARRAY -> AnnotationModel.Value.Array.Double(vals.map { AnnotationModel.Value.Double(it.value as Double) })
            else -> {
                require(p is ParameterizedTypeName)
                val componentType = p.typeArguments.single().toKotlinVersion()
                AnnotationModel.Value.Array.Object(
                    elementType = componentType,
                    value = vals.map { value ->
                        value.accept(AnnotationModelValueVisitor(metadataAccessor), componentType) as AnnotationModel.Value.Object<*>
                    }
                )
            }
        }

    companion object {
        fun AnnotationMirror.toAnnotationModel(metadataAccessor: MetadataAccessor): AnnotationModel {
            val metadata = metadataAccessor.getMetadataOrNull(annotationType.asElement())
                ?.let(metadataAccessor::getKmClass)

            val typesByName = metadata
                ?.constructors
                ?.find { !it.isSecondary }
                ?.valueParameters
                ?.associateBy({ it.name }) { it.type.toAnnotationTypeName() }
                ?: emptyMap()

            return AnnotationModel(
                annotationName = annotationType.asTypeName() as ClassName,
                hasMethods = MoreTypes.asTypeElement(annotationType).enclosedElements.isNotEmpty(),
                values = elementValues.entries.associateBy({ it.key.simpleName.toString() }) { (k, v) ->
                    v.accept(
                        AnnotationModelValueVisitor(metadataAccessor),
                        typesByName[k.simpleName.toString()] ?: k.returnType.asTypeName()
                    )
                }
            )
        }
    }
}

private fun KmType.toAnnotationTypeName(): TypeName {
    val argumentList = arguments.map { it.toTypeName() }
    val type = when (val valClassifier = classifier) {
        is KmClassifier.TypeParameter -> throw AssertionError("Annotations are never generic")
        is KmClassifier.Class -> {
            flexibleTypeUpperBound?.toTypeName()?.let { return it }
            outerType?.toAnnotationTypeName()?.let { return it }
            val rawType = createClassName(valClassifier.name)
            if (argumentList.isNotEmpty()) {
                rawType.parameterizedBy(argumentList)
            } else {
                rawType
            }
        }
        is KmClassifier.TypeAlias -> createClassName(valClassifier.name)
    }

    val finalType = type.copy(nullable = isNullable)
    return abbreviatedType
        ?.toAnnotationTypeName()
        ?.copy(tags = mapOf(TypeAliasTag::class to TypeAliasTag(finalType)))
        ?: finalType
}

private fun KmTypeProjection.toTypeName(): TypeName {
    val typename = type?.toAnnotationTypeName() ?: STAR
    return when (variance) {
        KmVariance.IN -> WildcardTypeName.consumerOf(typename)
        KmVariance.OUT -> WildcardTypeName.producerOf(typename)
        KmVariance.INVARIANT -> typename
        null -> STAR
    }
}

private fun KmFlexibleTypeUpperBound.toTypeName(): TypeName = WildcardTypeName.producerOf(type.toAnnotationTypeName())
