plugins {
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.5.0"
}

allprojects {
    repositories {
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
