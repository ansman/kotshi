
import org.gradle.jvm.tasks.Jar
import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    id("library")
    id("maven-publish")
    id("signing")
    id("org.jetbrains.dokka")
    id("se.ansman.sonatype-publish-fix")
}

version = providers.gradleProperty("version").get()
group = "se.ansman.kotshi"

fun MavenPublication.printPublishedInfo() {
    println("Published artifact $groupId:$artifactId:$version")
}

fun ExtraPropertiesExtension.getOrPut(name: String, block: () -> String): String =
    if (has(name)) get(name) as String else block().also { set(name, it) }

tasks.withType<DokkaTask>().configureEach {
    moduleVersion.set(version.toString())
    dokkaSourceSets.configureEach {
        externalDocumentationLink { url.set(uri("https://square.github.io/okio/2.x/okio/").toURL()) }
        externalDocumentationLink { url.set(uri("https://square.github.io/moshi/1.x/moshi/").toURL()) }
        sourceLink {
            localDirectory.set(file("src/main/kotlin"))
            remoteUrl.set(
                uri("https://github.com/ansman/kotshi/blob/main/${name}/src/main/kotlin").toURL()
            )
            remoteLineSuffix.set("#L")
        }
    }
}

tasks.withType<AbstractPublishToMaven>().configureEach {
    doLast { publication.printPublishedInfo() }
}

val sourcesJar = tasks.register<Jar>("sourcesJar") {
    dependsOn("classes")
    archiveClassifier.set("sources")
    from(sourceSets.getByName("main").allSource)
}

val dokkaJavadoc = tasks.named("dokkaJavadoc")
val dokkaJavadocJar = tasks.register<Jar>("dokkaJavadocJar") {
    from(dokkaJavadoc)
    archiveClassifier.set("javadoc")
}

val publication = with(the<PublishingExtension>()) {
    repositories.maven {
        name = "mavenCentral"
        setUrl("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
        credentials.apply {
            username = providers.gradleProperty("sonatype.oss.sonatype.org.username")
                .orElse(providers.environmentVariable("SONATYPE_USERNAME"))
                .orNull
            password = providers.gradleProperty("sonatype.oss.sonatype.org.password")
                .orElse(providers.environmentVariable("SONATYPE_PASSWORD"))
                .orNull
        }
    }

    repositories.maven {
        name = "sonatypeSnapshots"
        setUrl("https://oss.sonatype.org/content/repositories/snapshots/")
        with(credentials) {
            username = providers.gradleProperty("sonatype.oss.sonatype.org.username")
                .orElse(providers.environmentVariable("SONATYPE_USERNAME"))
                .orNull
            password = providers.gradleProperty("sonatype.oss.sonatype.org.password")
                .orElse(providers.environmentVariable("SONATYPE_PASSWORD"))
                .orNull
        }
    }

    publications.register<MavenPublication>("kotshi") {
        from(components["java"])
        artifact(sourcesJar)
        artifact(dokkaJavadocJar)

        with(pom) {
            name.set("Kotshi ${project.name}")
            description.set("An annotations processor that generates Moshi adapters from Kotlin data classes")
            url.set("https://github.com/ansman/kotshi")
            licenses {
                license {
                    name.set("The Apache Software License, Version 2.0")
                    url.set("https://github.com/ansman/kotshi/blob/main/LICENSE.txt")
                    distribution.set("repo")
                }
            }
            developers {
                developer {
                    id.set("ansman")
                    name.set("Nicklas Ansman Giertz")
                    email.set("nicklas@ansman.se")
                }
            }
            scm {
                connection.set("scm:git:git://github.com/ansman/kotshi.git")
                developerConnection.set("scm:git:ssh://git@github.com/ansman/kotshi.git")
                url.set("https://github.com/ansman/kotshi")
            }
        }
    }
}

if (providers.gradleProperty("signArtifacts").orNull?.toBooleanStrict() == true) {
    configure<SigningExtension> {
        useGpgCmd()
        sign(publication.get())
    }
}

tasks.register("publishSnapshot") {
    if (version.toString().endsWith("-SNAPSHOT")) {
        dependsOn("publishAllPublicationsToSonatypeSnapshotsRepository")
    }
}

pluginManager.withPlugin("org.jetbrains.kotlin.kapt") {
    tasks.named("dokkaJavadoc") {
        dependsOn("compileKotlin")
    }
}