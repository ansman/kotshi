package se.ansman.kotshi

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import com.squareup.moshi.JsonAdapter
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.SourceFile.Companion.java
import com.tschuchort.compiletesting.SourceFile.Companion.kotlin
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import se.ansman.kotshi.Errors.ignoredDataClassPropertyWithoutDefaultValue
import se.ansman.kotshi.Errors.javaClassNotSupported
import se.ansman.kotshi.Errors.jsonDefaultValueAppliedToInvalidType
import se.ansman.kotshi.Errors.multipleJsonDefaultValueInEnum
import se.ansman.kotshi.Errors.nestedSealedClassHasPolymorphicLabel
import se.ansman.kotshi.Errors.nestedSealedClassMissingPolymorphicLabel
import se.ansman.kotshi.Errors.nestedSealedClassMustBePolymorphic
import se.ansman.kotshi.Errors.nonIgnoredDataClassPropertyMustNotBeTransient
import se.ansman.kotshi.Errors.privateClass
import se.ansman.kotshi.Errors.privateDataClassConstructor
import se.ansman.kotshi.Errors.privateDataClassProperty
import se.ansman.kotshi.Errors.transientDataClassPropertyWithoutDefaultValue
import se.ansman.kotshi.Errors.unsupportedSerializableType
import se.ansman.kotshi.assertions.isAssignableTo
import java.io.File

@OptIn(ExperimentalCompilerApi::class)
abstract class BaseGeneratorTest {
    @TempDir
    lateinit var temporaryFolder: File

    protected abstract val processorClassName: String
    protected open val extraGeneratedFiles: List<File> get() = emptyList()

    @Test
    fun `data class must not be private`() {
        val result = compile(kotlin("source.kt", """
            @se.ansman.kotshi.JsonSerializable
            private data class Foo(val property: String)
        """))
        assertThat(result::exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result::messages).contains(privateClass)
    }

    @Test
    fun `data class must not have a private constructor`() {
        @Suppress("DataClassPrivateConstructor")
        val result = compile(kotlin("source.kt", """
            @se.ansman.kotshi.JsonSerializable
            data class Foo private constructor(val property: String)
        """))
        assertThat(result::exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result::messages).contains(privateDataClassConstructor)
    }

    @Test
    fun `data class properties must not be private`() {
        val result = compile(kotlin("source.kt", """
            @se.ansman.kotshi.JsonSerializable
            data class Foo(private val property: String)
        """))
        assertThat(result::exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result::messages).contains(privateDataClassProperty("property"))
    }

    @Test
    fun `transient data class properties must have default values`() {
        val result = compile(kotlin("source.kt", """
            @se.ansman.kotshi.JsonSerializable
            data class Foo(@Transient val property: String)
        """))
        assertThat(result::exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result::messages).contains(transientDataClassPropertyWithoutDefaultValue("property"))
    }

    @Test
    fun `ignored data class properties must have default values`() {
        if (usingLegacyMoshi) return
        val result = compile(kotlin("source.kt", """
            @se.ansman.kotshi.JsonSerializable
            data class Foo(@com.squareup.moshi.Json(ignore = true) val property: String)
        """))
        assertThat(result::exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result::messages).contains(ignoredDataClassPropertyWithoutDefaultValue("property"))
    }

    @Test
    fun `non ignored data class properties must not be transient`() {
        if (usingLegacyMoshi) return
        val result = compile(kotlin("source.kt", """
            @se.ansman.kotshi.JsonSerializable
            data class Foo(@com.squareup.moshi.Json(ignore = false) @Transient val property: String)
        """))
        assertThat(result::exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result::messages).contains(nonIgnoredDataClassPropertyMustNotBeTransient("property"))
    }

    @Test
    fun `sealed classes must be annotated with @Polymorphic`() {
        val result = compile(kotlin("source.kt", """
            @se.ansman.kotshi.JsonSerializable
            sealed class SealedClass {
              @se.ansman.kotshi.JsonSerializable
              @se.ansman.kotshi.PolymorphicLabel("implementation")
              data class Implementation(val value: String) : SealedClass()
            }
        """))
        assertThat(result::exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result::messages).contains(Errors.sealedClassMustBePolymorphic)
    }

    @Test
    fun `polymorphic subclass must not be generic unless subclass is`() {
        val result = compile(kotlin("source.kt", """
            @se.ansman.kotshi.JsonSerializable
            @se.ansman.kotshi.Polymorphic("type")
            sealed class SealedClass {
              @se.ansman.kotshi.JsonSerializable
              @se.ansman.kotshi.PolymorphicLabel("implementation")
              data class Implementation<T>(val value: T) : SealedClass()
            }
        """))
        assertThat(result::exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result::messages).contains(Errors.sealedSubclassMustNotHaveGeneric("T"))
    }

    @Test
    fun `default polymorphic values must not be generic`() {
        val result = compile(kotlin("source.kt", """
            @se.ansman.kotshi.JsonSerializable
            @se.ansman.kotshi.Polymorphic("type")
            sealed class SealedClass<T> {
              @se.ansman.kotshi.JsonSerializable
              @se.ansman.kotshi.JsonDefaultValue
              data class Implementation<T>(val value: T) : SealedClass<T>()
            }
        """))
        assertThat(result::exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result::messages).contains(Errors.defaultSealedValueIsGeneric)
    }

    @Test
    fun `cannot have multiple default polymorphic values`() {
        val result = compile(kotlin("source.kt", """
            @se.ansman.kotshi.JsonSerializable
            @se.ansman.kotshi.Polymorphic("type")
            sealed class SealedClass {
              @se.ansman.kotshi.JsonSerializable
              @se.ansman.kotshi.PolymorphicLabel("implementation")
              @se.ansman.kotshi.JsonDefaultValue
              data class Implementation(val value: String) : SealedClass()

              @se.ansman.kotshi.JsonSerializable
              @se.ansman.kotshi.PolymorphicLabel("default")
              @se.ansman.kotshi.JsonDefaultValue
              data object Default : SealedClass()
            }
        """))
        assertThat(result::exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result::messages).contains(Errors.multipleJsonDefaultValueInSealedClass)
    }

    @Test
    fun `polymorphic classes must be annotated with @JsonSerializable`() {
        val result = compile(kotlin("source.kt", """
            @se.ansman.kotshi.Polymorphic("type")
            sealed class SealedClass {
              @se.ansman.kotshi.JsonSerializable
              @se.ansman.kotshi.PolymorphicLabel("implementation")
              data class Implementation(val value: String) : SealedClass()
            }
        """))
        assertThat(result::exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result::messages).contains(Errors.polymorphicClassMustHaveJsonSerializable)
    }

    @Test
    fun `polymorphic classes must have at least one implementation`() {
        val result = compile(kotlin("source.kt", """
            @se.ansman.kotshi.JsonSerializable
            @se.ansman.kotshi.Polymorphic("type")
            sealed class SealedClass
        """))
        assertThat(result::exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result::messages).contains(Errors.noSealedSubclasses)
    }

    @Test
    fun `polymorphic subclasses classes must be annotated with @JsonSerializable`() {
        val result = compile(kotlin("source.kt", """
            @se.ansman.kotshi.Polymorphic("type")
            @se.ansman.kotshi.JsonSerializable
            sealed class SealedClass {
              @se.ansman.kotshi.PolymorphicLabel("implementation")
              data class Implementation(val value: String) : SealedClass()
            }
        """))
        assertThat(result::exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result::messages).contains(Errors.polymorphicSubclassMustHaveJsonSerializable)
    }

    @Test
    fun `polymorphic subclasses classes must be annotated with @PolymorphicLabel`() {
        val result = compile(kotlin("source.kt", """
            @se.ansman.kotshi.Polymorphic("type")
            @se.ansman.kotshi.JsonSerializable
            sealed class SealedClass {
              @se.ansman.kotshi.JsonSerializable
              data class Implementation(val value: String) : SealedClass()
            }
        """))
        assertThat(result::exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result::messages).contains(Errors.polymorphicSubclassMustHavePolymorphicLabel)
    }

    @Test
    fun `cannot apply @JsonSerializable to inner classes`() {
        val result = compile(kotlin("source.kt", """
         class Wrapper {
           @se.ansman.kotshi.JsonSerializable
           sealed inner class Inner(val value: String)
         }
        """))
        assertThat(result::exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result::messages).contains(Errors.dataClassCannotBeInner)
    }

    @Test
    fun `objects must not be private`() {
        val result = compile(kotlin("source.kt", """
           @se.ansman.kotshi.JsonSerializable
           private data object Singleton
        """))
        assertThat(result::exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result::messages).contains(privateClass)
    }

    @Test
    fun `sealed classes must not be private`() {
        val result = compile(kotlin("source.kt", """
           @se.ansman.kotshi.Polymorphic("type")
           @se.ansman.kotshi.JsonSerializable
           private sealed class SealedClass {
             @se.ansman.kotshi.PolymorphicLabel("implementation")
             @se.ansman.kotshi.JsonSerializable
             data class Implementation(val value: String) : SealedClass()
           }
        """))
        assertThat(result::exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result::messages).contains(privateClass)
    }

    @Test
    fun `sealed classes cannot be default values`() {
        val result = compile(kotlin("source.kt", """
           @se.ansman.kotshi.Polymorphic("type")
           @se.ansman.kotshi.JsonSerializable
           sealed class SealedClass {
             @se.ansman.kotshi.Polymorphic("type")
             @se.ansman.kotshi.JsonSerializable
             @se.ansman.kotshi.JsonDefaultValue
             sealed class Nested : SealedClass() {
               @se.ansman.kotshi.PolymorphicLabel("implementation")
               @se.ansman.kotshi.JsonSerializable
               data class Implementation(val value: String) : Nested()
             }
           }
        """))
        assertThat(result::exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result::messages).contains(jsonDefaultValueAppliedToInvalidType)
    }

    @Test
    fun `nested sealed classes must be annotated with @Polymorphic`() {
        val result = compile(kotlin("source.kt", """
           @se.ansman.kotshi.Polymorphic("type")
           @se.ansman.kotshi.JsonSerializable
           sealed class SealedClass {
             @se.ansman.kotshi.JsonSerializable
             sealed class Nested : SealedClass() {
               @se.ansman.kotshi.PolymorphicLabel("implementation")
               @se.ansman.kotshi.JsonSerializable
               data class Implementation(val value: String) : Nested()
             }
           }
        """))
        assertThat(result::exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result::messages).contains(nestedSealedClassMustBePolymorphic)
    }

    @Test
    fun `nested sealed classes with duplicate label keys must not have polymorphic label`() {
        val result = compile(kotlin("source.kt", """
           @se.ansman.kotshi.Polymorphic("type")
           @se.ansman.kotshi.JsonSerializable
           sealed class SealedClass {
             @se.ansman.kotshi.JsonSerializable
             @se.ansman.kotshi.Polymorphic("type")
             @se.ansman.kotshi.PolymorphicLabel("nested")
             sealed class Nested : SealedClass() {
               @se.ansman.kotshi.PolymorphicLabel("implementation")
               @se.ansman.kotshi.JsonSerializable
               data class Implementation(val value: String) : Nested()
             }
           }
        """))
        assertThat(result::exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result::messages).contains(nestedSealedClassHasPolymorphicLabel)
    }

    @Test
    fun `nested sealed classes with separate label keys must have polymorphic label`() {
        val result = compile(kotlin("source.kt", """
           @se.ansman.kotshi.Polymorphic("type")
           @se.ansman.kotshi.JsonSerializable
           sealed class SealedClass {
             @se.ansman.kotshi.JsonSerializable
             @se.ansman.kotshi.Polymorphic("subtype")
             sealed class Nested : SealedClass() {
               @se.ansman.kotshi.PolymorphicLabel("implementation")
               @se.ansman.kotshi.JsonSerializable
               data class Implementation(val value: String) : SealedClass()
             }
           }
        """))
        assertThat(result::exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result::messages).contains(nestedSealedClassMissingPolymorphicLabel)
    }

    @Test
    fun `enum classes must not be private`() {
        val result = compile(kotlin("source.kt", """
            @se.ansman.kotshi.JsonSerializable
            private enum class SomeEnum {
              Value1,
            }
        """))
        assertThat(result::exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result::messages).contains(privateClass)
    }

    @Test
    fun `only one enum can be annotated with @JsonDefaultValue`() {
        val result = compile(kotlin("source.kt", """
            @se.ansman.kotshi.JsonSerializable
            enum class SomeEnum {
              @se.ansman.kotshi.JsonDefaultValue
              Value1,
              @se.ansman.kotshi.JsonDefaultValue
              Value2
            }
        """))
        assertThat(result::exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result::messages).contains(multipleJsonDefaultValueInEnum)
    }

    @Test
    fun `cannot serialize interfaces`() {
        val result = compile(kotlin("source.kt", """
            @se.ansman.kotshi.JsonSerializable
            interface Interface
        """))
        assertThat(result::exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result::messages).contains(unsupportedSerializableType)
    }

    @Test
    fun `java classes are not supported`() {
        val result = compile(java("JavaType.java", """
            @se.ansman.kotshi.JsonSerializable
            class JavaType {}
        """))
        assertThat(result::exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result::messages).contains(javaClassNotSupported)
    }

    @Test
    fun `cannot have multiple factories`() {
        val result = compile(kotlin("source.kt", """
            @se.ansman.kotshi.KotshiJsonAdapterFactory
            object Factory1 : com.squareup.moshi.JsonAdapter.Factory
            @se.ansman.kotshi.KotshiJsonAdapterFactory
            object Factory2 : com.squareup.moshi.JsonAdapter.Factory
        """))
        assertThat(result::exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result::messages).contains(Errors.multipleFactories(listOf("Factory1", "Factory2")))
    }

    @Test
    fun `factories can be objects`() {
        val result = compile(kotlin("source.kt", """
            @se.ansman.kotshi.KotshiJsonAdapterFactory
            object Factory : com.squareup.moshi.JsonAdapter.Factory by KotshiFactory
        """))
        assertThat(result::exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        val factory = result.tryLoadClass("KotshiFactory")
        if (factory != null) {
            assertThat(factory).isAssignableTo<JsonAdapter.Factory>()
        }
    }

    @Test
    fun `factories cannot be written in java`() {
        val result = compile(java("Factory.java", """
            @se.ansman.kotshi.KotshiJsonAdapterFactory
            interface Factory extends com.squareup.moshi.JsonAdapter.Factory {}
        """))
        assertThat(result::exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result::messages).contains(javaClassNotSupported)
    }

    @Test
    fun `registered adapters cannot be written in java`() {
        val result = compile(java("Adapter.java", """
            @se.ansman.kotshi.KotshiJsonAdapterFactory
            abstract class Adapter extends com.squareup.moshi.JsonAdapter<String> {}
        """))
        assertThat(result::exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result::messages).contains(javaClassNotSupported)
    }

    @Test
    fun `registered adapters must be objects or classes`() {
        val result = compile(kotlin("source.kt", """
            @se.ansman.kotshi.RegisterJsonAdapter
            abstract class ManuallyRegisteredAdapter : com.squareup.moshi.JsonAdapter<ManuallyRegisteredAdapter.Type>() {
                object Type
            }
        """))
        assertThat(result::exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result::messages).contains(Errors.invalidRegisterAdapterType)
    }

    @Test
    fun `registered adapters must be public or internal`() {
        val result = compile(kotlin("source.kt", """
            @se.ansman.kotshi.RegisterJsonAdapter
            private object ManuallyRegisteredAdapter : com.squareup.moshi.JsonAdapter<ManuallyRegisteredAdapter.Type>() {
                override fun fromJson(reader: com.squareup.moshi.JsonReader): Type = throw UnsupportedOperationException()
                override fun toJson(writer: com.squareup.moshi.JsonWriter, value: Type?) = throw UnsupportedOperationException()
                object Type
            }
        """))
        assertThat(result::exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result::messages).contains(Errors.invalidRegisterAdapterVisibility)
    }

    @Test
    fun `cannot register adapter without factory`() {
        val result = compile(kotlin("source.kt", """
            @se.ansman.kotshi.RegisterJsonAdapter
            object ManuallyRegisteredAdapter : com.squareup.moshi.JsonAdapter<ManuallyRegisteredAdapter.Type>() {
                override fun fromJson(reader: com.squareup.moshi.JsonReader): Type = throw UnsupportedOperationException()
                override fun toJson(writer: com.squareup.moshi.JsonWriter, value: Type?) = throw UnsupportedOperationException()
                object Type
            }
        """))
        assertThat(result::exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result::messages).contains(Errors.registeredAdapterWithoutFactory)
    }

    @Test
    fun `can add jdk 8 generated annotation`() {
        val generated = java("Generated.java", """
            package javax.annotation;
            import java.lang.annotation.*;
            import static java.lang.annotation.ElementType.*;
            import static java.lang.annotation.RetentionPolicy.*;
            
            @Documented
            @Retention(SOURCE)
            @Target({PACKAGE, TYPE, METHOD, CONSTRUCTOR, FIELD, LOCAL_VARIABLE, PARAMETER})
            public @interface Generated {
                String[] value();
                String date() default "";
                String comments() default "";
            }
        """)
        val source = kotlin("source.kt", """
            @se.ansman.kotshi.JsonSerializable
            data object TestObject

            @se.ansman.kotshi.KotshiJsonAdapterFactory
            object TestFactory : com.squareup.moshi.JsonAdapter.Factory by KotshiTestFactory
        """)
        val annotationClass = "javax.annotation.Generated"
        val result = compile(generated, source, options = mapOf("kotshi.generatedAnnotation" to annotationClass))
        assertThat(result::exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        result.generatedFiles
            .plus(extraGeneratedFiles)
            .filter { it.name.endsWith(".kt") && "Kotshi" in it.name }
            .map { it.readText() }
            .onEach {
                assertThat(it).contains("@Generated")
                assertThat(it).contains("\"$processorClassName\"")
                assertThat(it).contains("comments = \"https://github.com/ansman/kotshi\"")
            }
            .let { assertThat(it).hasSize(2) }
    }

    @Test
    fun `can add jdk 9 generated annotation`() {
        val source = kotlin("source.kt", """
            @se.ansman.kotshi.JsonSerializable
            data object TestObject

            @se.ansman.kotshi.KotshiJsonAdapterFactory
            object TestFactory : com.squareup.moshi.JsonAdapter.Factory by KotshiTestFactory
        """)
        val annotationClass = "javax.annotation.processing.Generated"
        val result = compile(source, options = mapOf("kotshi.generatedAnnotation" to annotationClass))
        assertThat(result::exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        result.generatedFiles
            .plus(extraGeneratedFiles)
            .filter { it.name.endsWith(".kt") && "Kotshi" in it.name }
            .map { it.readText() }
            .forEach {
                assertThat(it).contains("@Generated")
                assertThat(it).contains("\"$processorClassName\"")
                assertThat(it).contains("comments = \"https://github.com/ansman/kotshi\"")
            }
    }

    @Test
    fun `cannot add invalid generated annotation`() {
        val source = kotlin("source.kt", """
            @se.ansman.kotshi.JsonSerializable
            data object TestObject

            @se.ansman.kotshi.KotshiJsonAdapterFactory
            object TestFactory : com.squareup.moshi.JsonAdapter.Factory by KotshiTestFactory
        """)
        val annotationClass = "foo.bar.Generated"
        val result = compile(source, options = mapOf("kotshi.generatedAnnotation" to annotationClass))
        assertThat(result::exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result::messages).contains(Errors.invalidGeneratedAnnotation(annotationClass))
    }

    @Test
    fun `non data object fails the build`() {
        val source = kotlin("source.kt", """
            @se.ansman.kotshi.JsonSerializable
            object TestObject
        """)
        val result = compile(source)
        assertThat(result::exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result::messages).contains(Errors.nonDataObject)
    }

    @Test
    fun `non data object does not logs warnings when using Kotlin 1_8`() {
        val source = kotlin("source.kt", """
            @se.ansman.kotshi.JsonSerializable
            object TestObject
        """)
        val result = compile(source, languageVersion = "1.8")
        assertThat(result::exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        assertThat(result::messages).doesNotContain(Errors.nonDataObject)
    }

    @Test
    fun `data class can use escaped Kotlin identifiers`() {
        val result = compile(kotlin("source.kt", """
            @se.ansman.kotshi.JsonSerializable
            data class Foo(val `in`: String)
        """))
        assertThat(result::exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        val kotshiAdapter = result.tryLoadClass("KotshiFooJsonAdapter")
        if (kotshiAdapter != null) {
            assertThat(kotshiAdapter).isAssignableTo(result.classLoader.loadClass("com.squareup.moshi.JsonAdapter"))
        }
    }


    protected fun compile(
        vararg sources: SourceFile,
        options: Map<String, String> = emptyMap(),
        languageVersion: String? = null
    ) =
        KotlinCompilation()
            .apply {
                workingDir = temporaryFolder
                this.sources = sources.asList()
                inheritClassPath = true
                this.languageVersion = languageVersion
                this.apiVersion = languageVersion
                messageOutputStream = System.out // see diagnostics in real time
                setUp(options)
            }
            .compile()

    protected abstract fun KotlinCompilation.setUp(options: Map<String, String>)
    protected open fun JvmCompilationResult.tryLoadClass(name: String): Class<*>? = classLoader.loadClass(name)
    protected fun JvmCompilationResult.getSourceByName(name: String): String =
        sourcesGeneratedByAnnotationProcessor.plus(extraGeneratedFiles)
            .first { it.name == name }
            .readText()
            .trim()
}