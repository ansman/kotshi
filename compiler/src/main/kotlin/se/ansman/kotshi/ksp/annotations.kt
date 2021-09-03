package se.ansman.kotshi.ksp

import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.BOOLEAN_ARRAY
import com.squareup.kotlinpoet.BYTE
import com.squareup.kotlinpoet.BYTE_ARRAY
import com.squareup.kotlinpoet.CHAR
import com.squareup.kotlinpoet.CHAR_ARRAY
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.DOUBLE_ARRAY
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.FLOAT_ARRAY
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.INT_ARRAY
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.LONG_ARRAY
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.SHORT_ARRAY
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.U_BYTE
import com.squareup.kotlinpoet.U_BYTE_ARRAY
import com.squareup.kotlinpoet.U_INT
import com.squareup.kotlinpoet.U_INT_ARRAY
import com.squareup.kotlinpoet.U_LONG
import com.squareup.kotlinpoet.U_LONG_ARRAY
import com.squareup.kotlinpoet.U_SHORT
import com.squareup.kotlinpoet.U_SHORT_ARRAY
import com.squareup.moshi.JsonQualifier
import se.ansman.kotshi.Types
import se.ansman.kotshi.model.AnnotationModel
import se.ansman.kotshi.model.AnnotationModel.Value

inline fun <reified T : Annotation> KSAnnotated.getAnnotation(): KSAnnotation? = getAnnotation(T::class.java)

fun KSAnnotated.getAnnotation(type: Class<out Annotation>): KSAnnotation? =
    annotations.getAnnotation(type)

inline fun <reified T : Annotation> Sequence<KSAnnotation>.getAnnotation(): KSAnnotation? = getAnnotation(T::class.java)

fun Sequence<KSAnnotation>.getAnnotation(type: Class<out Annotation>): KSAnnotation? =
    firstOrNull {
        it.shortName.asString() == type.simpleName &&
            it.annotationType.resolve().declaration.qualifiedName?.asString() == type.name
    }

inline fun <reified V> KSAnnotation.getValue(name: String): V =
    arguments.first { it.name?.asString() == name }.value as V

inline fun <reified V : Enum<V>> KSAnnotation.getEnumValue(name: String, defaultValue: V): V =
    getValue<KSType?>(name)?.let { enumValueOf<V>(it.declaration.simpleName.getShortName()) } ?: defaultValue

fun KSAnnotation.isJsonQualifier(): Boolean =
    annotationType.resolve().declaration.annotations.any {
        it.shortName.asString() == Types.Moshi.jsonQualifier.simpleName &&
            it.annotationType.resolve().declaration.qualifiedName?.asString() == JsonQualifier::class.java.name
    }

fun KSAnnotation.toAnnotationModel(): AnnotationModel {
    val typeByName = annotationType.resolve().declaration.let { it as KSClassDeclaration }
        .primaryConstructor
        ?.parameters
        ?.associateBy({ it.name!! }, {
            if (it.isVararg) {
                LIST.parameterizedBy(it.type.asTypeName())
            } else {
                it.type.asTypeName()
            }
        })
        ?: emptyMap()

    val annotationType = annotationType.resolve()
    return AnnotationModel(
        annotationName = annotationType.asTypeName() as ClassName,
        hasMethods = (annotationType.declaration as KSClassDeclaration).getAllProperties().any(),
        values = arguments.filter { it.value != null }.associateBy({ it.name!!.asString() }) {
            it.value!!.toAnnotationValue(it, typeByName.getValue(it.name!!))
        }
    )
}

private fun Any.toAnnotationValue(node: KSNode, type: TypeName): Value<*> =
    when (this) {
        is KSType -> {
            val declaration = declaration as KSClassDeclaration
            if (declaration.classKind == ClassKind.ENUM_ENTRY) {
                Value.Enum((declaration.parentDeclaration as KSClassDeclaration).asClassName(), declaration.simpleName.asString())
            } else {
                Value.Class(declaration.asClassName())
            }
        }
        is KSName -> Value.Enum(
            enumType = ClassName.bestGuess(getQualifier()),
            value = getShortName(),
        )
        is KSAnnotation -> Value.Annotation(toAnnotationModel())
        is String -> Value.String(this)
        is Float -> Value.Float(this)
        is Char -> Value.Char(this)
        is Boolean -> Value.Boolean(this)
        is Double -> Value.Double(this)
        is Byte -> Value.Byte(this)
        is UByte -> Value.UByte(this)
        is Short -> Value.Short(this)
        is UShort -> Value.UShort(this)
        is Int -> Value.Int(this)
        is UInt -> Value.UInt(this)
        is Long -> Value.Long(this)
        is ULong -> Value.ULong(this)
        is List<*> -> when (type) {
            FLOAT, FLOAT_ARRAY -> Value.Array.Float(map { Value.Float(it as Float) })
            CHAR, CHAR_ARRAY -> Value.Array.Char(map { Value.Char(it as Char) })
            BOOLEAN, BOOLEAN_ARRAY -> Value.Array.Boolean(map { Value.Boolean(it as Boolean) })
            DOUBLE, DOUBLE_ARRAY -> Value.Array.Double(map { Value.Double(it as Double) })
            BYTE, BYTE_ARRAY -> Value.Array.Byte(map { Value.Byte(it as Byte) })
            U_BYTE, U_BYTE_ARRAY -> Value.Array.UByte(map { Value.UByte((it as Byte).toUByte()) })
            SHORT, SHORT_ARRAY -> Value.Array.Short(map { Value.Short(it as Short) })
            U_SHORT, U_SHORT_ARRAY -> Value.Array.UShort(map { Value.UShort((it as Short).toUShort()) })
            INT, INT_ARRAY -> Value.Array.Int(map { Value.Int(it as Int) })
            U_INT, U_INT_ARRAY -> Value.Array.UInt(map { Value.UInt((it as Int).toUInt()) })
            LONG, LONG_ARRAY -> Value.Array.Long(map { Value.Long(it as Long) })
            U_LONG, U_LONG_ARRAY -> Value.Array.ULong(map { Value.ULong((it as Long).toULong()) })
            else -> {
                require(type is ParameterizedTypeName) {
                    "Expected $type (${type.javaClass}) to be a ParameterizedTypeName"
                }
                val elementType = type.typeArguments.single()
                Value.Array.Object(
                    elementType = if (elementType is ParameterizedTypeName && elementType.rawType == Types.Kotlin.kClass) {
                        Types.Java.clazz.parameterizedBy(elementType.typeArguments)
                    } else {
                        elementType
                    },
                    value = map { it!!.toAnnotationValue(node, type) as Value.Object<*> }
                )
            }
        }
        else -> throw KspProcessingError("Unknown annotation value type $javaClass", node)
    }