import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `published-library`
}

dependencies {
    api(deps.moshi.oldestSupported)
    testImplementation(deps.junit)
    testImplementation(deps.truth)
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-opt-in=kotlin.RequiresOptIn"
        )
    }
}

tasks.compileTestKotlin {
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-Xjvm-default=all"
        )
    }
}