package se.ansman.kotshi

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.JsonAdapter
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.SourceFile.Companion.java
import com.tschuchort.compiletesting.SourceFile.Companion.kotlin
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
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

abstract class BaseGeneratorTest {
    @Rule
    @JvmField
    val temporaryFolder: TemporaryFolder = TemporaryFolder()

    @Test
    fun `data class must not be private`() {
        val result = compile(kotlin("source.kt", """
            @se.ansman.kotshi.JsonSerializable
            private data class Foo(val property: String)
        """.trimIndent()))
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(privateClass)
    }

    @Test
    fun `data class must not have a private constructor`() {
        @Suppress("DataClassPrivateConstructor")
        val result = compile(kotlin("source.kt", """
            @se.ansman.kotshi.JsonSerializable
            data class Foo private constructor(val property: String)
        """.trimIndent()))
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(privateDataClassConstructor)
    }

    @Test
    fun `data class properties must not be private`() {
        val result = compile(kotlin("source.kt", """
            @se.ansman.kotshi.JsonSerializable
            data class Foo(private val property: String)
        """.trimIndent()))
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(privateDataClassProperty("property"))
    }

    @Test
    fun `transient data class properties must have default values`() {
        val result = compile(kotlin("source.kt", """
            @se.ansman.kotshi.JsonSerializable
            data class Foo(@Transient val property: String)
        """.trimIndent()))
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(transientDataClassPropertyWithoutDefaultValue("property"))
    }

    @Test
    fun `ignored data class properties must have default values`() {
        val result = compile(kotlin("source.kt", """
            @se.ansman.kotshi.JsonSerializable
            data class Foo(@com.squareup.moshi.Json(ignore = true) val property: String)
        """.trimIndent()))
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(ignoredDataClassPropertyWithoutDefaultValue("property"))
    }

    @Test
    fun `non ignored data class properties must not be transient`() {
        val result = compile(kotlin("source.kt", """
            @se.ansman.kotshi.JsonSerializable
            data class Foo(@com.squareup.moshi.Json(ignore = false) @Transient val property: String)
        """.trimIndent()))
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(nonIgnoredDataClassPropertyMustNotBeTransient("property"))
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
        """.trimIndent()))
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(Errors.sealedClassMustBePolymorphic)
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
        """.trimIndent()))
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(Errors.sealedSubclassMustNotHaveGeneric("T"))
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
        """.trimIndent()))
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(Errors.defaultSealedValueIsGeneric)
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
              object Default : SealedClass()
            }
        """.trimIndent()))
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(Errors.multipleJsonDefaultValueInSealedClass)
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
        """.trimIndent()))
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(Errors.polymorphicClassMustHaveJsonSerializable)
    }

    @Test
    fun `polymorphic classes must have at least one implementation`() {
        val result = compile(kotlin("source.kt", """
            @se.ansman.kotshi.JsonSerializable
            @se.ansman.kotshi.Polymorphic("type")
            sealed class SealedClass
        """.trimIndent()))
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(Errors.noSealedSubclasses)
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
        """.trimIndent()))
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(Errors.polymorphicSubclassMustHaveJsonSerializable)
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
        """.trimIndent()))
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(Errors.polymorphicSubclassMustHavePolymorphicLabel)
    }

    @Test
    fun `cannot apply @JsonSerializable to inner classes`() {
        val result = compile(kotlin("source.kt", """
         class Wrapper {
           @se.ansman.kotshi.JsonSerializable
           sealed inner class Inner(val value: String)
         }
        """.trimIndent()))
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(Errors.dataClassCannotBeInner)
    }

    @Test
    fun `objects must not be private`() {
        val result = compile(kotlin("source.kt", """
           @se.ansman.kotshi.JsonSerializable
           private object Singleton
        """.trimIndent()))
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(privateClass)
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
        """.trimIndent()))
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(privateClass)
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
        """.trimIndent()))
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(jsonDefaultValueAppliedToInvalidType)
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
        """.trimIndent()))
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(nestedSealedClassMustBePolymorphic)
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
        """.trimIndent()))
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(nestedSealedClassHasPolymorphicLabel)
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
        """.trimIndent()))
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(nestedSealedClassMissingPolymorphicLabel)
    }

    @Test
    fun `enum classes must not be private`() {
        val result = compile(kotlin("source.kt", """
            @se.ansman.kotshi.JsonSerializable
            private enum class SomeEnum {
              Value1,
            }
        """.trimIndent()))
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(privateClass)
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
        """.trimIndent()))
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(multipleJsonDefaultValueInEnum)
    }

    @Test
    fun `cannot serialize interfaces`() {
        val result = compile(kotlin("source.kt", """
            @se.ansman.kotshi.JsonSerializable
            interface Interface
        """.trimIndent()))
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(unsupportedSerializableType)
    }

    @Test
    fun `java classes are not supported`() {
        val result = compile(java("JavaType.java", """
            @se.ansman.kotshi.JsonSerializable
            class JavaType {}
        """.trimIndent())
        )
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(javaClassNotSupported)
    }

    @Test
    fun `cannot have multiple factories`() {
        val result = compile(kotlin("source.kt", """
            @se.ansman.kotshi.KotshiJsonAdapterFactory
            object Factory1 : com.squareup.moshi.JsonAdapter.Factory
            @se.ansman.kotshi.KotshiJsonAdapterFactory
            object Factory2 : com.squareup.moshi.JsonAdapter.Factory
        """.trimIndent())
        )
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(Errors.multipleFactories(listOf("Factory1", "Factory2")))
    }

    @Test
    fun `factories can be abstract classes`() {
        val result = compile(kotlin("source.kt", """
            @se.ansman.kotshi.KotshiJsonAdapterFactory
            abstract class Factory : com.squareup.moshi.JsonAdapter.Factory
        """.trimIndent())
        )
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        val kotshiFactory = result.tryLoadClass("KotshiFactory")
        if (kotshiFactory != null) {
            assertThat(kotshiFactory).isAssignableTo(result.classLoader.loadClass("Factory"))
        }
    }

    @Test
    fun `factories can be objects`() {
        val result = compile(kotlin("source.kt", """
            @se.ansman.kotshi.KotshiJsonAdapterFactory
            object Factory : com.squareup.moshi.JsonAdapter.Factory by KotshiFactory
        """.trimIndent())
        )
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        val factory = result.tryLoadClass("KotshiFactory")
        if (factory != null) {
            assertThat(factory).isAssignableTo(JsonAdapter.Factory::class.java)
        }
    }

    @Test
    fun `factories can be interfaces`() {
        val result = compile(kotlin("source.kt", """
            @se.ansman.kotshi.KotshiJsonAdapterFactory
            interface Factory : com.squareup.moshi.JsonAdapter.Factory
        """.trimIndent())
        )
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        assertThat(result.messages).contains(Errors.abstractFactoriesAreDeprecated)
        val kotshiFactory = result.tryLoadClass("KotshiFactory")
        if (kotshiFactory != null) {
            assertThat(kotshiFactory).isAssignableTo(result.classLoader.loadClass("Factory"))
        }
    }

    @Test
    fun `factories cannot be written in java`() {
        val result = compile(java("Factory.java", """
            @se.ansman.kotshi.KotshiJsonAdapterFactory
            interface Factory extends com.squareup.moshi.JsonAdapter.Factory {}
        """.trimIndent())
        )
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(javaClassNotSupported)
    }

    @Test
    fun `registered adapters cannot be written in java`() {
        val result = compile(java("Adapter.java", """
            @se.ansman.kotshi.KotshiJsonAdapterFactory
            abstract class Adapter extends com.squareup.moshi.JsonAdapter<String> {}
        """.trimIndent())
        )
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(javaClassNotSupported)
    }

    @Test
    fun `registered adapters must be objects or classes`() {
        val result = compile(kotlin("source.kt", """
            @se.ansman.kotshi.RegisterJsonAdapter
            abstract class ManuallyRegistedAdapter : com.squareup.moshi.JsonAdapter<ManuallyRegistedAdapter.Type>() {
                object Type
            }
        """.trimIndent())
        )
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(Errors.invalidRegisterAdapterType)
    }

    @Test
    fun `registered adapters must be public or internal`() {
        val result = compile(kotlin("source.kt", """
            @se.ansman.kotshi.RegisterJsonAdapter
            private object ManuallyRegistedAdapter : com.squareup.moshi.JsonAdapter<ManuallyRegistedAdapter.Type>() {
                override fun fromJson(reader: com.squareup.moshi.JsonReader): Type = throw UnsupportedOperationException()
                override fun toJson(writer: com.squareup.moshi.JsonWriter, value: Type?) = throw UnsupportedOperationException()
                object Type
            }
        """.trimIndent())
        )
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(Errors.invalidRegisterAdapterVisibility)
    }

    @Test
    fun `cannot register adapter without factory`() {
        val result = compile(kotlin("source.kt", """
            @se.ansman.kotshi.RegisterJsonAdapter
            object ManuallyRegistedAdapter : com.squareup.moshi.JsonAdapter<ManuallyRegistedAdapter.Type>() {
                override fun fromJson(reader: com.squareup.moshi.JsonReader): Type = throw UnsupportedOperationException()
                override fun toJson(writer: com.squareup.moshi.JsonWriter, value: Type?) = throw UnsupportedOperationException()
                object Type
            }
        """.trimIndent())
        )
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(Errors.registeredAdapterWithoutFactory)
    }

    protected fun compile(vararg sources: SourceFile) =
        KotlinCompilation()
            .apply {
                workingDir = temporaryFolder.root
                this.sources = sources.asList()
                inheritClassPath = true
                messageOutputStream = System.out // see diagnostics in real time
                setUp()
            }
            .compile()

    protected abstract fun KotlinCompilation.setUp()
    protected abstract fun KotlinCompilation.Result.tryLoadClass(name: String): Class<*>?
}