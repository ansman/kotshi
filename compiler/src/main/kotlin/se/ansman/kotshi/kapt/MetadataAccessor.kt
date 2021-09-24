package se.ansman.kotshi.kapt

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.DelicateKotlinPoetApi
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.metadata.specs.ClassInspector
import com.squareup.kotlinpoet.metadata.specs.toTypeSpec
import com.squareup.kotlinpoet.metadata.toKmClass
import com.squareup.kotlinpoet.tag
import kotlinx.metadata.KmClass
import kotlinx.metadata.isLocal
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement

class MetadataAccessor(private val classInspector: ClassInspector) {
    private val metadataPerType = mutableMapOf<ClassName, KmClass>()
    private val typeSpecPerKmClass = mutableMapOf<KmClass, TypeSpec>()

    fun getMetadata(type: Element): KmClass =
        @OptIn(DelicateKotlinPoetApi::class) // OK because we are using the class name for comparisson
        metadataPerType.getOrPut((type as TypeElement).asClassName()) {
            type.getAnnotation(Metadata::class.java)
                ?.toKmClass()
                ?: throw KaptProcessingError("Class must be written in Kotlin", type)
        }

    fun getTypeSpec(type: Element): TypeSpec = getTypeSpec(getMetadata(type))

    fun getTypeSpec(metadata: KmClass): TypeSpec =
        typeSpecPerKmClass.getOrPut(metadata) {
            metadata.toTypeSpec(classInspector)
                .toBuilder()
                .tag(createClassName(metadata.name))
                .build()
        }
}

fun createClassName(kotlinMetadataName: String): ClassName {
    require(!kotlinMetadataName.isLocal) {
        "Local/anonymous classes are not supported!"
    }
    // Top-level: package/of/class/MyClass
    // Nested A:  package/of/class/MyClass.NestedClass
    val simpleName = kotlinMetadataName.substringAfterLast(
        '/', // Drop the package name, e.g. "package/of/class/"
        '.' // Drop any enclosing classes, e.g. "MyClass."
    )
    val packageName = kotlinMetadataName.substringBeforeLast(
        delimiter = "/",
        missingDelimiterValue = ""
    )
    val simpleNames = kotlinMetadataName.removeSuffix(simpleName)
        .removeSuffix(".") // Trailing "." if any
        .removePrefix(packageName)
        .removePrefix("/")
        .let {
            if (it.isNotEmpty()) {
                it.split(".")
            } else {
                // Don't split, otherwise we end up with an empty string as the first element!
                emptyList()
            }
        }
        .plus(simpleName)

    return ClassName(
        packageName = packageName.replace("/", "."),
        simpleNames = simpleNames
    )
}

@Suppress("SameParameterValue")
private fun String.substringAfterLast(vararg delimiters: Char): String {
    val index = lastIndexOfAny(delimiters)
    return if (index == -1) this else substring(index + 1, length)
}