import org.jetbrains.kotlin.gradle.dsl.KaptArguments

plugins {
    `test-library`
    kotlin("kapt")
}

dependencies {
    kapt(projects.compiler)
}

fun KaptArguments.argFromGradleProperty(name: String) {
    val value = providers.gradleProperty(name).orNull
    if (value != null) {
        arg(name, value)
    }
}