import org.jetbrains.kotlin.gradle.dsl.KaptArguments

plugins {
    `test-library`
    kotlin("kapt")
}

dependencies {
    kapt(projects.compiler)
}

tasks.compileKotlin {
    libraries.from(buildDir.resolve("tmp/kapt3/classes/main"))
}

fun KaptArguments.argFromGradleProperty(name: String) {
    val value = providers.gradleProperty(name).orNull
    if (value != null) {
        arg(name, value)
    }
}

kapt {
    arguments {
        argFromGradleProperty("kotshi.createAnnotationsUsingConstructor")
        argFromGradleProperty("kotshi.useLegacyDataClassRenderer")
    }
}