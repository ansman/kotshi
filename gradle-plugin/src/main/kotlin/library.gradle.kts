import org.gradle.accessors.dm.LibrariesForLibs
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
}

val libs = the<LibrariesForLibs>()

extensions.configure<JavaPluginExtension> {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(19))
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-Xjvm-default=all"
        )
    }
}

tasks.withType<Javadoc> { enabled = false }

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.kotlin.test.junit)
}