package se.ansman.kotshi.gradle

import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer

internal val Project.sourceSets: SourceSetContainer
    get() = property("sourceSets") as SourceSetContainer

internal inline fun Project.sourceSets(configure: SourceSetContainer.() -> Unit) {
    sourceSets.apply(configure)
}