package se.ansman.kotshi.gradle

import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

abstract class LibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.apply("org.jetbrains.kotlin.jvm")

        target.tasks.withType(KotlinCompile::class.java) { task ->
            task.kotlinOptions {
                jvmTarget = "1.8"
            }
        }
        target.extensions.configure(JavaPluginExtension::class.java) { extension ->
            extension.sourceCompatibility = JavaVersion.VERSION_1_8
            extension.targetCompatibility = JavaVersion.VERSION_1_8
        }

        target.tasks.withType(JavaCompile::class.java) { task ->
            task.sourceCompatibility = "1.8"
            task.targetCompatibility = "1.8"
        }
    }
}