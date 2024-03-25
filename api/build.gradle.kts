import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    id("published-library")
}

dependencies {
    api(libs.oldestSupportedMoshi)
}

val packagesMarkdown = layout.buildDirectory.file("generated/docs/packages.md")
val buildPackagesDocs by tasks.registering {
    val readme = rootDir.resolve("README.md")
    inputs.file(readme)
    outputs.file(packagesMarkdown)
    doFirst {
        packagesMarkdown.get().asFile.writer().use { writer ->
            writer.write("# Module kotshi\n\n")
            readme.reader().use { reader ->
                reader.copyTo(writer)
            }
        }
    }
}

tasks.withType<DokkaTask>().configureEach {
    dependsOn(buildPackagesDocs)
    moduleName.set("kotshi")
    dokkaSourceSets {
        configureEach {
            includes.from(packagesMarkdown)
        }
    }
}