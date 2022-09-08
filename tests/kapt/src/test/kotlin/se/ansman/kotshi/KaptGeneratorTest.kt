package se.ansman.kotshi

import com.tschuchort.compiletesting.KotlinCompilation
import se.ansman.kotshi.kapt.KotshiProcessor

class KaptGeneratorTest : BaseGeneratorTest() {
    override fun KotlinCompilation.setUp() {
        annotationProcessors = listOf(KotshiProcessor())
    }

    override fun KotlinCompilation.Result.tryLoadClass(name: String): Class<*> = classLoader.loadClass(name)
}