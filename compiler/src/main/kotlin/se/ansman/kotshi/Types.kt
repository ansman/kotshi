package se.ansman.kotshi

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.MemberName.Companion.member
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.moshi.*
import com.squareup.moshi.Types
import java.io.IOException
import java.lang.reflect.Type

@OptIn(InternalKotshiApi::class, DelicateKotlinPoetApi::class)
object Functions {
    object Kotlin {
        val typeOf = MemberName("kotlin.reflect", "typeOf")
        val setOf = MemberName("kotlin.collections", "setOf")
        val apply = MemberName("kotlin", "apply")
        val javaType = MemberName("kotlin.reflect", "javaType")
        val booleanArrayOf = MemberName("kotlin", "booleanArrayOf")
        val byteArrayOf = MemberName("kotlin", "byteArrayOf")
        val ubyteArrayOf = MemberName("kotlin", "ubyteArrayOf")
        val charArrayOf = MemberName("kotlin", "charArrayOf")
        val shortArrayOf = MemberName("kotlin", "shortArrayOf")
        val ushortArrayOf = MemberName("kotlin", "ushortArrayOf")
        val intArrayOf = MemberName("kotlin", "intArrayOf")
        val uintArrayOf = MemberName("kotlin", "uintArrayOf")
        val longArrayOf = MemberName("kotlin", "longArrayOf")
        val ulongArrayOf = MemberName("kotlin", "ulongArrayOf")
        val doubleArrayOf = MemberName("kotlin", "doubleArrayOf")
        val floatArrayOf = MemberName("kotlin", "floatArrayOf")
        val arrayOf = MemberName("kotlin", "arrayOf")
    }

    object Kotshi {
        val byteValue = KotshiUtils::class.member("byteValue")
        val value = KotshiUtils::class.member("value")
        val nextFloat = KotshiUtils::class.member("nextFloat")
        val nextByte = KotshiUtils::class.member("nextByte")
        val nextShort = KotshiUtils::class.member("nextShort")
        val nextChar = KotshiUtils::class.member("nextChar")
        val appendNullableError = KotshiUtils::class.member("appendNullableError")
        val typeArgumentsOrFail = KotshiUtils::class.java.member("typeArgumentsOrFail")
        val createJsonQualifierImplementation = KotshiUtils::class.member("createJsonQualifierImplementation")
        val matches = KotshiUtils::class.member("matches")
    }

    object Moshi {
        val newParameterizedType = Types::class.member("newParameterizedType")
        val getRawType = Types::class.member("getRawType")
    }
}

@OptIn(InternalKotshiApi::class, DelicateKotlinPoetApi::class)
object Types {
    object Kotlin {
        val string = STRING
        val array = ARRAY
        val set = SET
        val annotation = ClassName("kotlin", "Annotation")
        val transient = ClassName("kotlin.jvm", "Transient")
        val suppress = ClassName("kotlin", "Suppress")
        val optIn = ClassName("kotlin", "OptIn")
        val kClass = ClassName("kotlin.reflect", "KClass")
        val experimentalStdlibApi = ClassName("kotlin", "ExperimentalStdlibApi")
    }

    object Kotshi {
        val internalKotshiApi = ClassName("se.ansman.kotshi", "InternalKotshiApi")
        val namedJsonAdapter = NamedJsonAdapter::class.java.asClassName()
        val jsonDefaultValue = JsonDefaultValue::class.java.asClassName()
        @OptIn(ExperimentalKotshiApi::class)
        val jsonProperty = JsonProperty::class.java.asClassName()
        val typesArray = ARRAY.parameterizedBy(Java.type)
    }

    object Moshi {
        val moshi = com.squareup.moshi.Moshi::class.java.asClassName()
        val json = Json::class.java.asClassName()
        val jsonQualifier = JsonQualifier::class.java.asClassName()
        val jsonAdapter = JsonAdapter::class.java.asClassName()
        val jsonAdapterFactory = JsonAdapter.Factory::class.java.asClassName()
        val jsonDataException = JsonDataException::class.java.asClassName()
        val jsonReaderOptions = JsonReader.Options::class.java.asClassName()
        val jsonReaderToken = JsonReader.Token::class.java.asClassName()
        val jsonWriter = JsonWriter::class.java.asClassName()
        val jsonReader = JsonReader::class.java.asClassName()

    }

    object Java {
        val generatedJDK9 = ClassName("javax.annotation.processing", "Generated")
        val generatedPreJDK9 = ClassName("javax.annotation", "Generated")
        val clazz = ClassName("java.lang", "Class")
        val string = ClassName("java.lang", "String")
        val type = Type::class.java.asClassName()
        val void = Void::class.java.asClassName()
        val ioException = IOException::class.java.asClassName()
    }
}