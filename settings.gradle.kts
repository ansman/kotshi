pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenLocal()
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
        mavenLocal()
    }
}

if (System.getenv("CI") != null) {
    extensions.findByName("buildScan")?.withGroovyBuilder {
        setProperty("termsOfServiceUrl", "https://gradle.com/terms-of-service")
        setProperty("termsOfServiceAgree", "yes")
    }
}