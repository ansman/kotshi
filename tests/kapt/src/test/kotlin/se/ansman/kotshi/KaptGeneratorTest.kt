package se.ansman.kotshi

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import se.ansman.kotshi.kapt.KotshiProcessor

@OptIn(ExperimentalCompilerApi::class)
class KaptGeneratorTest : BaseGeneratorTest() {
    override val processorClassName: String get() = KotshiProcessor::class.java.canonicalName

    override fun KotlinCompilation.setUp(options: Map<String, String>) {
        annotationProcessors = listOf(KotshiProcessor())
        kaptArgs.putAll(options)
        useKapt4 = true
    }

    override fun JvmCompilationResult.tryLoadClass(name: String): Class<*> = classLoader.loadClass(name)
}