package se.ansman.kotshi

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.kspArgs
import com.tschuchort.compiletesting.kspIncremental
import com.tschuchort.compiletesting.symbolProcessorProviders
import se.ansman.kotshi.kapt.KotshiProcessor
import se.ansman.kotshi.ksp.KotshiSymbolProcessor
import se.ansman.kotshi.ksp.KotshiSymbolProcessorProvider
import java.io.File

class KspGeneratorTest : BaseGeneratorTest() {
    override val processorClassName: String get() = KotshiSymbolProcessor::class.java.canonicalName

    override val extraGeneratedFiles: List<File>
        get() = temporaryFolder.root.resolve("ksp/sources/kotlin/").listFiles()?.asList() ?: emptyList()

    override fun KotlinCompilation.setUp(options: Map<String, String>) {
        kspIncremental = true
        symbolProcessorProviders = listOf(KotshiSymbolProcessorProvider())
        kspArgs.putAll(options)
    }

    // https://github.com/tschuchortdev/kotlin-compile-testing/issues/312
    override fun KotlinCompilation.Result.tryLoadClass(name: String): Class<*>? = null
}