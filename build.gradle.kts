import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.5.0"
}

allprojects {
    repositories {
        google()
        mavenCentral()
        mavenLocal()
    }
    tasks.withType<Javadoc> { enabled = false }
    tasks.withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = freeCompilerArgs + listOf(
                "-Xopt-in=kotlin.RequiresOptIn"
            )
        }
    }
}

apiValidation {
    allprojects.filterNot { it.path == ":api" }.mapTo(ignoredProjects) { it.name }
}

tasks.register("publishSnapshot") {
    if (version.toString().endsWith("-SNAPSHOT")) {
        for (project in allprojects) {
            project.tasks.findByName("publishAllPublicationsToSonatypeSnapshotsRepository")?.let { dependsOn(it) }
        }
    }
}