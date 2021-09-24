plugins {
    `published-library`
    kotlin("kapt")
}

tasks.compileKotlin {
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-Xopt-in=com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview",
            "-Xopt-in=com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview",
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
    implementation(deps.moshi)
    implementation(deps.ksp.api)
}
