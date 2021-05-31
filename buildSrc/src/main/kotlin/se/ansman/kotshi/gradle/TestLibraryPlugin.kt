package se.ansman.kotshi.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

abstract class TestLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.apply(LibraryPlugin::class.java)

        target.sourceSets {
            getByName("main") {
                it.java.srcDir(target.rootProject.rootDir.resolve("tests/src/main/kotlin"))
            }
            getByName("test") {
                it.java.srcDir(target.rootProject.rootDir.resolve("tests/src/test/kotlin"))
            }
        }

        with(target.dependencies) {
            add("implementation", target.project(":api"))
            add("compileOnly", deps.findBugs)
            add("testImplementation", deps.truth)
            add("testImplementation", deps.compiletesting)
            add("testImplementation", deps.junit)
            add("testImplementation", deps.kotlin.junit)
            add("testImplementation", deps.moshi)
        }
    }
}