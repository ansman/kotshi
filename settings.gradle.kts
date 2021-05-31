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