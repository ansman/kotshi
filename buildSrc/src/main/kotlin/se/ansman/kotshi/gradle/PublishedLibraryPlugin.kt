package se.ansman.kotshi.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.tasks.userinput.UserInputHandler
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.configurationcache.extensions.serviceOf
import org.gradle.jvm.tasks.Jar
import org.gradle.plugins.signing.SigningExtension
import org.jetbrains.dokka.gradle.DokkaTask

abstract class PublishedLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply(LibraryPlugin::class.java)
        pluginManager.apply("maven-publish")
        pluginManager.apply("signing")
        pluginManager.apply("org.jetbrains.dokka")

        version = target.providers.gradleProperty("version").get()
        group = "se.ansman.kotshi"

        tasks.named("dokkaJavadoc", DokkaTask::class.java) { task ->
            task.dokkaSourceSets.configureEach { sourceSet ->
                sourceSet.reportUndocumented.set(false)
                sourceSet.externalDocumentationLink(
                    url = "https://square.github.io/moshi/1.x/moshi/",
                    packageListUrl = "https://square.github.io/moshi/1.x/moshi/package-list"
                )
                sourceSet.sourceLink { sourceLink ->
                    sourceLink.localDirectory.set(target.file("src/main/kotlin"))
                    sourceLink.remoteUrl.set(
                        target.uri("https://github.com/ansman/kotshi/blob/main/${target.name}/src/main/kotlin").toURL()
                    )
                    sourceLink.remoteLineSuffix.set("#L")
                }
            }
        }

        tasks.withType(AbstractPublishToMaven::class.java).configureEach { task ->
            task.doLast { task.publication.printPublishedInfo() }
        }

        val sourcesJar = tasks.register("sourcesJar", Jar::class.java) { task ->
            task.dependsOn("classes")
            task.archiveClassifier.set("sources")
            task.from(sourceSets.getByName("main").allSource)
        }

        val dokkaJavadoc = tasks.named("dokkaJavadoc")
        val dokkaJavadocJar = tasks.register("dokkaJavadocJar", Jar::class.java) { task ->
            task.from(dokkaJavadoc)
            task.archiveClassifier.set("javadoc")
        }

        val publication = with(target.extensions.getByType(PublishingExtension::class.java)) {
            repositories.maven { repo ->
                repo.name = "mavenCentral"
                repo.setUrl("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
                with(repo.credentials) {
                    username = providers.gradleProperty("sonatype.username")
                        .orElse(providers.environmentVariable("SONATYPE_USERNAME"))
                        .orNull
                    password = providers.gradleProperty("sonatype.password")
                        .orElse(providers.environmentVariable("SONATYPE_PASSWORD"))
                        .orNull
                }
            }

            repositories.maven { repo ->
                repo.name = "sonatypeSnapshots"
                repo.setUrl("https://oss.sonatype.org/content/repositories/snapshots/")
                with(repo.credentials) {
                    username = providers.gradleProperty("sonatype.username")
                        .orElse(providers.environmentVariable("SONATYPE_USERNAME"))
                        .orNull
                    password = providers.gradleProperty("sonatype.password")
                        .orElse(providers.environmentVariable("SONATYPE_PASSWORD"))
                        .orNull
                }
            }

            publications.register("kotshi", MavenPublication::class.java) { publication ->
                publication.from(project.components.getByName("java"))
                publication.artifact(sourcesJar)
                publication.artifact(dokkaJavadocJar)

                publication.pom { pom ->
                    pom.name.set("Kotshi ${project.name}")
                    pom.description.set("An annotations processor that generates Moshi adapters from Kotlin data classes")
                    pom.url.set("https://github.com/ansman/kotshi")
                    pom.licenses {
                        it.license { license ->
                            with(license) {
                                name.set("The Apache Software License, Version 2.0")
                                url.set("https://github.com/ansman/kotshi/blob/main/LICENSE.txt")
                                distribution.set("repo")
                            }
                        }
                    }
                    pom.developers {
                        it.developer { developer ->
                            with(developer) {
                                id.set("ansman")
                                name.set("Nicklas Ansman Giertz")
                                email.set("nicklas@ansman.se")

                            }
                        }
                        pom.scm { scm ->
                            with(scm) {
                                connection.set("scm:git:git://github.com/ansman/kotshi.git")
                                developerConnection.set("scm:git:ssh://git@github.com/ansman/kotshi.git")
                                url.set("https://github.com/ansman/kotshi")
                            }
                        }
                    }
                }
            }
        }


        if (!providers.environmentVariable("CI").orNull.toBoolean()) {
            with(extensions.getByType(SigningExtension::class.java)) {
                gradle.taskGraph.whenReady { graph ->
                    if (graph.hasTask("${path}:sign${publication.name.replaceFirstChar(Char::uppercase)}Publication")) {
                        rootProject.extensions.extraProperties.getOrPut("signing.gnupg.passphrase") {
                            val inputHandler = serviceOf<UserInputHandler>()
                            inputHandler.askQuestion("Signing key passphrase: ", "")
                        }
                        useGpgCmd()
                    }
                }
                sign(publication.get())
            }
        }
    }

}

private fun MavenPublication.printPublishedInfo() {
    println("Published artifact $groupId:$artifactId:$version")
}

private fun ExtraPropertiesExtension.getOrPut(name: String, block: () -> String): String =
    if (has(name)) get(name) as String else block().also { set(name, it) }