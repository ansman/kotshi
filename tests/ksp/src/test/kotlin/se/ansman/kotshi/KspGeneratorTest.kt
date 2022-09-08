package se.ansman.kotshi

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.kspIncremental
import com.tschuchort.compiletesting.symbolProcessorProviders
import se.ansman.kotshi.ksp.KotshiSymbolProcessorProvider

class KspGeneratorTest : BaseGeneratorTest() {
    override fun KotlinCompilation.setUp() {
        kspIncremental = true
        symbolProcessorProviders = listOf(KotshiSymbolProcessorProvider())
    }

    // https://github.com/tschuchortdev/kotlin-compile-testing/issues/312
    override fun KotlinCompilation.Result.tryLoadClass(name: String): Class<*>? = null
}