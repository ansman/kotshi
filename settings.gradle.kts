pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
    }
}

plugins {
    id("com.gradle.enterprise") version "3.15"
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
}

gradleEnterprise {
    if (System.getenv("CI") != null) {
        buildScan {
            publishAlways()
            termsOfServiceUrl = "https://gradle.com/terms-of-service"
            termsOfServiceAgree = "yes"
        }
    }
}