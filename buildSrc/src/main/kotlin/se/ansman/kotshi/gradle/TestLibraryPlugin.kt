package se.ansman.kotshi.gradle

import deps
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

abstract class TestLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.apply(LibraryPlugin::class.java)

        target.sourceSets {
            getByName("main") {
                it.java.srcDir(target.rootDir.resolve("tests/src/main/kotlin"))
            }
            getByName("test") {
                it.java.srcDirs(target.rootDir.resolve("tests/src/test/kotlin"))
            }
        }
        target.tasks.withType(JavaCompile::class.java) { task ->
            task.sourceCompatibility = "11"
            task.targetCompatibility = "11"
        }
        target.extensions.configure(JavaPluginExtension::class.java) { extension ->
            extension.sourceCompatibility = JavaVersion.VERSION_11
            extension.targetCompatibility = JavaVersion.VERSION_11
        }

        target.tasks.withType(KotlinCompile::class.java) { task ->
            task.kotlinOptions {
                jvmTarget = "11"
            }
        }

        with(target.dependencies) {
            add("implementation", target.project(":api"))
            add("implementation", target.project(":compiler"))
            add("implementation", deps.moshi.current)
            add("compileOnly", deps.findBugs)
            add("testImplementation", deps.truth)
            add("testImplementation", deps.compileTesting.core)
            add("testImplementation", deps.junit)
            add("testImplementation", deps.kotlin.junit)
        }
    }
}