import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `published-library`
}

dependencies {
    api(deps.moshi)
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-Xopt-in=kotlin.RequiresOptIn"
        )
    }
}