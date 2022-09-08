import com.google.devtools.ksp.gradle.KspExtension

plugins {
    `test-library`
    id("com.google.devtools.ksp") version deps.ksp.version
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
    testImplementation(deps.compileTesting.ksp)
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