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