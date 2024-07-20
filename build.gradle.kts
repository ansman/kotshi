plugins {
    kotlin("jvm") version libs.versions.kotlin.get() apply false
    alias(libs.plugins.kotlinx.binaryCompatibilityValidator)
}

apiValidation {
    allprojects.filterNot { it.path == ":api" }.mapTo(ignoredProjects) { it.name }
}