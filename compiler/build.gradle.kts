plugins {
    `published-library`
    kotlin("kapt")
}

tasks.compileKotlin {
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-Xuse-experimental=com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview",
            "-Xuse-experimental=kotlin.Experimental",
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
    implementation(deps.kotlinpoet.metadataSpecs)
    implementation(deps.kotlinpoet.classinspector)
    implementation(deps.moshi)
}
