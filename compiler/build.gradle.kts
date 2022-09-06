plugins {
    `published-library`
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
    implementation(deps.autoService.api)
    kapt(deps.autoService.compiler)
    implementation(deps.incap.api)
    kapt(deps.incap.compiler)
    implementation(deps.autocommon)
    implementation(deps.kotlinpoet.core)
    implementation(deps.kotlinpoet.metadata)
    implementation(deps.kotlinpoet.ksp)
    implementation(deps.moshi.oldestSupported)
    implementation(deps.ksp.api)
    implementation(deps.asm)
}
