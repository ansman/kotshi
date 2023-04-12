import com.google.devtools.ksp.gradle.KspExtension

plugins {
    id("test-library")
    alias(libs.plugins.ksp)
}

kotlin {
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
    sourceSets.test {
        kotlin.srcDir("build/generated/ksp/test/kotlin")
    }
}

dependencies {
    ksp(projects.compiler)
    testImplementation(libs.compileTesting.ksp)
}

fun KspExtension.argFromGradleProperty(name: String) {
    val value = providers.gradleProperty(name).orNull
    if (value != null) {
        arg(name, value)
    }
}

ksp {
    argFromGradleProperty("kotshi.createAnnotationsUsingConstructor")
    argFromGradleProperty("kotshi.useLegacyDataClassRenderer")
}