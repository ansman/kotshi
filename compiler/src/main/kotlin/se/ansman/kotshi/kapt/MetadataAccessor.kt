package se.ansman.kotshi.kapt

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.DelicateKotlinPoetApi
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.metadata.specs.ClassInspector
import com.squareup.kotlinpoet.metadata.specs.toTypeSpec
import com.squareup.kotlinpoet.tag
import se.ansman.kotshi.Errors.javaClassNotSupported
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import kotlin.metadata.KmClass
import kotlin.metadata.isLocalClassName
import kotlin.metadata.jvm.KotlinClassMetadata

class MetadataAccessor(private val classInspector: ClassInspector) {
    private val metadataPerType = mutableMapOf<ClassName, Metadata?>()
    private val kmClassPerMetadata = mutableMapOf<Metadata, KmClass>()
    private val typeSpecPerKmClass = mutableMapOf<KmClass, TypeSpec>()

    @OptIn(DelicateKotlinPoetApi::class) // OK because we are using the class name for comparison
    fun getMetadataOrNull(type: Element): Metadata? =
        metadataPerType.getOrPut((type as TypeElement).asClassName()) {
            type.getAnnotation(Metadata::class.java)
        }

    fun getMetadata(type: Element): Metadata =
        getMetadataOrNull(type)
            ?: throw KaptProcessingError(javaClassNotSupported, type)

    fun getLanguageVersion(type: Element): KotlinVersion = getMetadata(type).languageVersion

    fun getKmClass(metadata: Metadata): KmClass = kmClassPerMetadata.getOrPut(metadata) {
        (KotlinClassMetadata.readLenient(metadata) as KotlinClassMetadata.Class).kmClass
    }
    fun getKmClass(type: Element): KmClass = getKmClass(getMetadata(type))
    fun getKmClassOrNull(type: Element): KmClass? = getMetadataOrNull(type)?.let(::getKmClass)

    fun getTypeSpec(type: Element): TypeSpec = getTypeSpec(getKmClass(type))
    fun getTypeSpecOrNull(type: Element): TypeSpec? = getKmClassOrNull(type)?.let(::getTypeSpec)

    fun getTypeSpec(metadata: KmClass): TypeSpec =
        typeSpecPerKmClass.getOrPut(metadata) {
            metadata.toTypeSpec(classInspector)
                .toBuilder()
                .tag(createClassName(metadata.name))
                .build()
        }
}

fun createClassName(kotlinMetadataName: String): ClassName {
    require(!kotlinMetadataName.isLocalClassName()) {
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

val Metadata.languageVersion: KotlinVersion
    get() = KotlinVersion(
        major = metadataVersion[0],
        minor = metadataVersion[1],
        patch = metadataVersion.getOrElse(2) { 0 },
    )

val Metadata.supportsCreatingAnnotationsWithConstructor: Boolean
    get() = languageVersion.isAtLeast(1, 6)