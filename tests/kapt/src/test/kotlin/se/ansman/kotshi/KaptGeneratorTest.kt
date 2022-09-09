package se.ansman.kotshi

import com.tschuchort.compiletesting.KotlinCompilation
import se.ansman.kotshi.kapt.KotshiProcessor

class KaptGeneratorTest : BaseGeneratorTest() {
    override val processorClassName: String get() = KotshiProcessor::class.java.canonicalName

    override fun KotlinCompilation.setUp(options: Map<String, String>) {
        annotationProcessors = listOf(KotshiProcessor())
        kaptArgs.putAll(options)
    }

    override fun KotlinCompilation.Result.tryLoadClass(name: String): Class<*> = classLoader.loadClass(name)
}