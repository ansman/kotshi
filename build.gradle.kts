plugins {
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.5.0"
}

allprojects {
    repositories {
        google()
        mavenCentral()
        mavenLocal()
    }
}

subprojects {
    tasks.withType<Javadoc> { enabled = false }
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