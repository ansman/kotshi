plugins {
    id("published-library")
    kotlin("kapt")
}

tasks.compileKotlin {
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-opt-in=com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview",
        )
    }
}

dependencies {
    implementation(projects.api)
    implementation(libs.auto.service.api)
    kapt(libs.auto.service.compiler)
    implementation(libs.incap.api)
    kapt(libs.incap.compiler)
    implementation(libs.auto.common)
    implementation(libs.kotlinpoet.core)
    implementation(libs.kotlinpoet.metadata)
    implementation(libs.kotlinpoet.ksp)
    implementation(libs.kotlinx.metadata)
    implementation(libs.moshi.oldestSupported)
    implementation(libs.ksp.api)
    implementation(libs.asm)
}
