plugins {
    id("published-library")
    kotlin("kapt")
    id("com.github.johnrengelman.shadow")
}

tasks.compileKotlin {
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-opt-in=com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview",
        )
    }
}

val shade by configurations.named("compileShaded")

@Suppress("UnstableApiUsage")
dependencies {
    implementation(projects.api)
    implementation(libs.auto.service.api)
    kapt(libs.auto.service.compiler)
    implementation(libs.incap.api)
    kapt(libs.incap.compiler)
    implementation(libs.auto.common)
    implementation(libs.kotlinpoet.core)
    shade(libs.kotlinpoet.metadata) {
        exclude("org.jetbrains.kotlin")
        exclude("com.squareup", "kotlinpoet")
        exclude("com.google.guava")
        exclude("com.google.auto", "auto-common")
    }
    implementation(libs.kotlinpoet.ksp)
    shade(libs.kotlinx.metadata) {
        exclude("org.jetbrains.kotlin", "kotlin-stdlib")
    }
    implementation(libs.oldestSupportedMoshi)
    implementation(libs.ksp.api)
    implementation(libs.asm)
}

tasks.shadowJar {
    relocate("com.squareup.kotlinpoet.metadata", "se.ansman.kotshi.compiler.kotlinpoet.metadata")
}