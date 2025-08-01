import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    id("library")
    id("org.jetbrains.dokka")
    id("org.jetbrains.dokka-javadoc")
    id("com.vanniktech.maven.publish")
    id("signing")
}

val group = "se.ansman.kotshi"
    .also { project.group = it }
val artifactId = name
val version = providers.gradleProperty("version").get()
    .also { project.version = it }

val gitCommit = project.providers
    .exec {
        commandLine("git", "rev-parse", "HEAD")
        workingDir = project.rootDir
    }
    .run {
        result.flatMap {
            it.assertNormalExitValue()
            it.rethrowFailure()
            standardOutput.asText
        }
    }
    .map { it.trim() }


tasks.withType<AbstractPublishToMaven>().configureEach {
    val group = project.group
    val artifactId = project.name
    val version = providers.gradleProperty("version").get()
    doLast {
        println("Published artifact $group:$artifactId:$version")
    }
}

val signArtifacts = providers.gradleProperty("signArtifacts").orNull?.toBooleanStrict() ?: false
mavenPublishing {
    coordinates(group, artifactId, version)
    publishToMavenCentral()

    configure(
        KotlinJvm(
            javadocJar = JavadocJar.Dokka(tasks.dokkaGeneratePublicationJavadoc.name),
            sourcesJar = true
        )
    )

    if (signArtifacts) {
        signAllPublications()
    }

    pom {
        name.set("Kotshi ${project.name}")
        description = "An annotations processor that generates Moshi adapters from Kotlin data classes"
        url = "https://github.com/ansman/kotshi"
        licenses {
            license {
                name = "The Apache Software License, Version 2.0"
                url = gitCommit.map { "https://github.com/ansman/kotshi/blob/$it/LICENSE.txt" }
                distribution = "repo"
            }
        }
        developers {
            developer {
                id = "ansman"
                name = "Nicklas Ansman"
                email = "nicklas@ansman.se"
            }
        }
        scm {
            connection = "scm:git:git://github.com/ansman/kotshi.git"
            developerConnection = "scm:git:ssh://git@github.com/ansman/kotshi.git"
            url = "https://github.com/ansman/kotshi"
        }
    }
}

if (signArtifacts) {
    signing {
        useGpgCmd()
    }
}

tasks.register("publishSnapshot") {
    if (providers.gradleProperty("version").get().endsWith("-SNAPSHOT")) {
        dependsOn("publishAllPublicationsToMavenCentralRepository")
    }
}

tasks.withType<DokkaTask>().configureEach {
    moduleVersion.set(version)
    dokkaSourceSets.configureEach {
        externalDocumentationLink { url.set(uri("https://square.github.io/okio/2.x/okio/").toURL()) }
        externalDocumentationLink { url.set(uri("https://square.github.io/moshi/1.x/moshi/").toURL()) }
        sourceLink {
            localDirectory.set(file("src/main/kotlin"))
            remoteUrl.set(
                gitCommit.map { uri("https://github.com/ansman/kotshi/blob/${it}/${name}/src/main/kotlin").toURL() }
            )
            remoteLineSuffix.set("#L")
        }
    }
}