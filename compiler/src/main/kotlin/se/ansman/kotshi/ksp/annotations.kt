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
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
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
    getValueOrDefault(name) {
        throw IllegalArgumentException("Annotation ${annotationType.resolve().declaration.qualifiedName?.asString()} is missing argument $name. Arguments: ${arguments.map { it.name?.asString() }}")
    }

inline fun <reified V> KSAnnotation.getValueOrDefault(name: String, defaultValue: () -> V): V {
    val arg = arguments.firstOrNull { it.name?.asString() == name } ?: return defaultValue()
    if (arg.value is V) {
        return arg.value as V
    } else {
        error("Expected ${arg.value} to be of type ${V::class.java} but was ${arg.value?.javaClass}")
    }
}

inline fun <reified V : Enum<V>> KSAnnotation.getEnumValue(name: String, defaultValue: V): V {
    val declaration = when (val value = getValue<Any?>(name)) {
        is KSType -> value.declaration
        is KSClassDeclaration -> value
        null -> return defaultValue
        else -> error("Expected $value to be a KSType or KSClassDeclaration but was ${value.javaClass}")
    }
    return enumValueOf<V>(declaration.simpleName.getShortName())
}

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
                LIST.parameterizedBy(it.type.toTypeName())
            } else {
                it.type.toTypeName()
            }
        })
        ?: emptyMap()

    val annotationType = annotationType.resolve()
    return AnnotationModel(
        annotationName = annotationType.toTypeName() as ClassName,
        hasMethods = (annotationType.declaration as KSClassDeclaration).getAllProperties().any(),
        values = arguments.filter { it.value != null }.associateBy({ it.name!!.asString() }) {
            it.value!!.toAnnotationValue(it, typeByName.getValue(it.name!!))
        }
    )
}

private fun Any.toAnnotationValue(node: KSNode, type: TypeName): Value<*> =
    when (this) {
        is KSClassDeclaration -> {
            if (classKind == ClassKind.ENUM_ENTRY) {
                Value.Enum(
                    (parentDeclaration as KSClassDeclaration).toClassName(),
                    simpleName.asString()
                )
            } else {
                Value.Class(toClassName())
            }
        }

        is KSType -> (declaration as KSClassDeclaration).toAnnotationValue(node, type)

        is KSName -> Value.Enum(
            enumType = ClassName.bestGuess(getQualifier()),
            value = getShortName(),
        )

        is KSAnnotation -> Value.Annotation(toAnnotationModel())
        is String -> Value.String(this)
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
                    elementType = elementType,
                    value = map { it!!.toAnnotationValue(node, type) as Value.Object<*> }
                )
            }
        }

        else -> when (type) {
            FLOAT -> Value.Float(this as Float)
            CHAR -> Value.Char(this as Char)
            BOOLEAN -> Value.Boolean(this as Boolean)
            DOUBLE -> Value.Double(this as Double)
            BYTE -> Value.Byte(this as Byte)
            U_BYTE -> Value.UByte((this as Byte).toUByte())
            SHORT -> Value.Short(this as Short)
            U_SHORT -> Value.UShort((this as Short).toUShort())
            INT -> Value.Int(this as Int)
            U_INT -> Value.UInt((this as Int).toUInt())
            LONG -> Value.Long(this as Long)
            U_LONG -> Value.ULong((this as Long).toULong())
            else -> throw KspProcessingError("Unknown annotation value type $javaClass", node)
        }
    }