plugins {
    kotlin("jvm") version embeddedKotlinVersion
    `java-gradle-plugin`
}

buildscript {
    repositories {
        mavenCentral()
    }
}

repositories {
    mavenCentral()
}

dependencies {
    api("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.10")
    api("org.jetbrains.dokka:dokka-gradle-plugin:1.4.32")
}

tasks.compileKotlin {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

gradlePlugin {
    plugins {
        register("library") {
            id = name
            implementationClass = "se.ansman.kotshi.gradle.LibraryPlugin"
        }
        register("published-library") {
            id = name
            implementationClass = "se.ansman.kotshi.gradle.PublishedLibraryPlugin"
        }
    }
}