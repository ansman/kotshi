plugins {
    id("published-library")
    kotlin("kapt")
}

dependencies {
    compileOnly(libs.auto.service.api)
    compileOnly(libs.ksp.api)

    implementation(projects.api)
    implementation(libs.kotlinpoet.core)
    implementation(libs.kotlinpoet.metadata)
    implementation(libs.kotlinpoet.ksp)
    implementation(libs.kotlin.metadata)
    implementation(libs.oldestSupportedMoshi)
    implementation(libs.asm)

    kapt(libs.auto.service.compiler)
}