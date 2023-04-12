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
    @Suppress("UnstableApiUsage")
    versionCatalogs {
        create("libs") {
            from(files("libs.versions.toml"))
        }
    }
}