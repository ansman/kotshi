pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
    }
}

rootProject.name = "kotshi"
include("compiler")
include("api")
include("tests:ksp")
include("tests:kapt")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
includeBuild("gradle-plugin")

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        google()
        mavenCentral()
    }

    versionCatalogs {
        create("libs") {
            from(file("gradle/libs.versions.toml"))
        }
    }
}